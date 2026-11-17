import {TestBed} from '@angular/core/testing';
import {HttpTestingController} from '@angular/common/http/testing';

import {commonTestProviders} from 'src/testing/test-providers';
import {Sesion} from '@app/core/interfaces/sesion.interface';
import {NegocioService} from './negocio.service';

const sesion = (id: number, rol: 'ADMIN_EMPRESA' | 'SUPER_ADMIN'): Sesion => ({
  accessToken: 'a', refreshToken: 'r', expiresIn: 900,
  usuario: {id, nombre: 'N', email: 'n@x.co', rol, empresaId: rol === 'SUPER_ADMIN' ? null : 7, empresaNombre: 'X', debeCambiarPassword: false}
});

describe('NegocioService', () => {
  let backend: HttpTestingController;

  const iniciar = (s: Sesion) => {
    localStorage.setItem('ac-sesion', JSON.stringify(s));
    TestBed.configureTestingModule({providers: commonTestProviders});
    backend = TestBed.inject(HttpTestingController);
    return TestBed.inject(NegocioService);
  };

  afterEach(() => {
    backend.verify();
    localStorage.clear();
  });

  it('pide el negocio una sola vez por administrador', () => {
    const negocio = iniciar(sesion(1, 'ADMIN_EMPRESA'));
    negocio.cargar();
    negocio.cargar();
    backend.expectOne('/admin/empresa').flush({nombre: 'Javier', logoUrl: null, linkPublico: '/c/javier-x', otro: 1});
    backend.expectOne('/admin/modulos').flush([]);
    expect(negocio.negocio()).toEqual({nombre: 'Javier', logoUrl: null, linkPublico: '/c/javier-x'});
  });

  it('pide los módulos una vez y responde si el negocio tiene uno', () => {
    const negocio = iniciar(sesion(1, 'ADMIN_EMPRESA'));
    expect(negocio.tiene('VENTA_RUTA')).toBe(false);
    let recibidos: Set<string> | null = null;
    negocio.cargarModulos().subscribe(m => recibidos = m);
    negocio.cargarModulos().subscribe();
    backend.expectOne('/admin/modulos').flush(['VENTA_RUTA', 'COLECCIONES']);
    expect(recibidos).toEqual(new Set(['VENTA_RUTA', 'COLECCIONES']));
    expect(negocio.tiene('VENTA_RUTA')).toBe(true);
    expect(negocio.tiene('IMPORTAR_EXCEL')).toBe(false);
  });

  it('el super admin no tiene negocio ni módulos', () => {
    const negocio = iniciar(sesion(2, 'SUPER_ADMIN'));
    negocio.cargar();
    backend.expectNone('/admin/empresa');
    backend.expectNone('/admin/modulos');
    expect(negocio.negocio()).toBeNull();
  });
});
