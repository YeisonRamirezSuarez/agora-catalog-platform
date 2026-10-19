import {inject} from '@angular/core';
import {CanActivateFn, Router} from '@angular/router';

import {Rol} from '@app/core/interfaces/sesion.interface';
import {AuthService} from '@app/core/services/auth.service';

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

/** El login, solo sin sesión. */
export const invitadoGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.usuario() ? inject(Router).parseUrl(auth.home()) : true;
};

/** El cambio de contraseña, solo con sesión. */
export const sesionGuard: CanActivateFn = () =>
  inject(AuthService).usuario() ? true : inject(Router).parseUrl('/login');
