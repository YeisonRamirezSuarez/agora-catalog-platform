import {TestBed} from '@angular/core/testing';
import {HttpClient, provideHttpClient, withInterceptors} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {provideRouter, Router} from '@angular/router';

import {Sesion} from '@app/core/interfaces/sesion.interface';
import {AuthService} from '@app/core/services/auth.service';
import {authInterceptor} from './auth.interceptor';

const sesion = (access: string, refresh: string): Sesion => ({
  accessToken: access,
  refreshToken: refresh,
  expiresIn: 900,
  usuario: {id: 1, nombre: 'Javier', email: 'j@x.co', rol: 'ADMIN_EMPRESA', empresaId: 7, empresaNombre: 'X', debeCambiarPassword: false}
});

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    localStorage.setItem('ac-sesion', JSON.stringify(sesion('acceso-1', 'refresh-1')));
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => {
    backend.verify();
    localStorage.clear();
  });

  it('agrega el token de acceso, pero no en el login', () => {
    http.get('/admin/empresa').subscribe();
    expect(backend.expectOne('/admin/empresa').request.headers.get('Authorization')).toBe('Bearer acceso-1');

    http.post('/auth/login', {}).subscribe();
    expect(backend.expectOne('/auth/login').request.headers.has('Authorization')).toBe(false);
  });

  it('con un 401 renueva la sesión una vez y repite la petición con el token nuevo', () => {
    let respuesta: unknown;
    http.get('/admin/empresa').subscribe(r => respuesta = r);

    backend.expectOne('/admin/empresa').flush({}, {status: 401, statusText: 'Unauthorized'});
    const refresh = backend.expectOne('/auth/refresh');
    expect(refresh.request.body).toEqual({refreshToken: 'refresh-1'});
    refresh.flush(sesion('acceso-2', 'refresh-2'));

    const repetida = backend.expectOne('/admin/empresa');
    expect(repetida.request.headers.get('Authorization')).toBe('Bearer acceso-2');
    repetida.flush({nombre: 'X'});

    expect(respuesta).toEqual({nombre: 'X'});
    expect(auth.accessToken).toBe('acceso-2');
  });

  it('si la renovación falla, cierra la sesión y lleva al login', () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    let fallo = false;
    http.get('/admin/empresa').subscribe({error: () => fallo = true});

    backend.expectOne('/admin/empresa').flush({}, {status: 401, statusText: 'Unauthorized'});
    backend.expectOne('/auth/refresh').flush({message: 'Tu sesión venció'}, {status: 401, statusText: 'Unauthorized'});

    expect(fallo).toBe(true);
    expect(auth.usuario()).toBeNull();
    expect(localStorage.getItem('ac-sesion')).toBeNull();
    expect(navegar).toHaveBeenCalledWith('/login');
  });

  it('cada rol entra a su inicio, y con contraseña temporal primero la cambia', () => {
    const u = sesion('a', 'r').usuario;
    expect(auth.home(u)).toBe('/admin');
    expect(auth.home({...u, rol: 'SUPER_ADMIN'})).toBe('/superadmin');
    expect(auth.home({...u, debeCambiarPassword: true})).toBe('/cambiar-contrasena');
    expect(auth.home(null)).toBe('/login');
  });
});
