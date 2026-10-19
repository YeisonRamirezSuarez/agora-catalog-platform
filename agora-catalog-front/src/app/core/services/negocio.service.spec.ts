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
    expect(negocio.negocio()).toEqual({nombre: 'Javier', logoUrl: null, linkPublico: '/c/javier-x'});
  });

  it('el super admin no tiene negocio', () => {
    const negocio = iniciar(sesion(2, 'SUPER_ADMIN'));
    negocio.cargar();
    backend.expectNone('/admin/empresa');
    expect(negocio.negocio()).toBeNull();
  });
});
