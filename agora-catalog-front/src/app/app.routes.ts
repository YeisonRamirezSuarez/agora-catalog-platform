import {Routes} from '@angular/router';

import {invitadoGuard, rolGuard, sesionGuard} from '@app/core/guards/auth.guard';

export const routes: Routes = [
  {path: '', pathMatch: 'full', redirectTo: 'login'},
  {
    path: 'login',
    canActivate: [invitadoGuard],
    title: 'Ingresar · Agorá Catalog',
    loadComponent: () => import('@app/modules/auth/login/login.component').then(c => c.LoginComponent)
  },
  {
    path: 'cambiar-contrasena',
    canActivate: [sesionGuard],
    title: 'Cambiar contraseña · Agorá Catalog',
    loadComponent: () => import('@app/modules/auth/cambiar-contrasena/cambiar-contrasena.component')
      .then(c => c.CambiarContrasenaComponent)
  },
  {
    path: 'superadmin',
    canActivate: [rolGuard('SUPER_ADMIN')],
    title: 'Empresas · Agorá Catalog',
    loadComponent: () => import('@app/modules/superadmin/empresas/empresas.component').then(c => c.EmpresasComponent)
  },
  {
    path: 'superadmin/correo',
    canActivate: [rolGuard('SUPER_ADMIN')],
    title: 'Correo · Agorá Catalog',
    loadComponent: () => import('@app/modules/superadmin/correo/correo.component').then(c => c.CorreoComponent)
  },
  {
    path: 'admin',
    canActivate: [rolGuard('ADMIN_EMPRESA')],
    title: 'Panel · Agorá Catalog',
    loadComponent: () => import('@app/modules/admin/inicio/inicio.component').then(c => c.InicioComponent)
  },
  {
    path: '**',
    loadComponent: () => import('@app/shared/components/not-found/not-found.component')
      .then(c => c.NotFoundComponent)
  }
];
