import {inject} from '@angular/core';
import {CanActivateFn, Router} from '@angular/router';

import {Rol} from '@app/core/interfaces/sesion.interface';
import {AuthService} from '@app/core/services/auth.service';
import {Modulo, NegocioService} from '@app/core/services/negocio.service';
import {catchError, map, of} from 'rxjs';

/**
 * Pantallas de un rol. Sin sesión, al login; con contraseña temporal, a cambiarla; con otro rol, a
 * su inicio. Es experiencia, no seguridad: quien decide es el backend con `@PreAuthorize`.
 */
export function rolGuard(rol: Rol): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const usuario = auth.usuario();
    if (usuario && !usuario.debeCambiarPassword && usuario.rol === rol) return true;
    return inject(Router).parseUrl(auth.home());
  };
}

/**
 * Pantallas de un módulo: sin él, al inicio del panel. Igual que `rolGuard`, es experiencia: el
 * backend responde 403 a una ruta de un módulo apagado.
 */
export function moduloGuard(modulo: Modulo): CanActivateFn {
  return () => {
    const router = inject(Router);
    return inject(NegocioService).cargarModulos().pipe(
      map(m => m.has(modulo) ? true : router.parseUrl('/admin')),
      catchError(() => of(router.parseUrl('/admin')))
    );
  };
}

/** El login, solo sin sesión. */
export const invitadoGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.usuario() ? inject(Router).parseUrl(auth.home()) : true;
};

/** El cambio de contraseña, solo con sesión. */
export const sesionGuard: CanActivateFn = () =>
  inject(AuthService).usuario() ? true : inject(Router).parseUrl('/login');
