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
    children: [
      {
        path: '', title: 'Panel · Agorá Catalog',
        loadComponent: () => import('@app/modules/admin/inicio/inicio.component').then(c => c.InicioComponent)
      },
      {
        path: 'productos', title: 'Productos · Agorá Catalog',
        loadComponent: () => import('@app/modules/admin/productos/productos.component').then(c => c.ProductosComponent)
      },
      {
        path: 'productos/nuevo', title: 'Nuevo producto · Agorá Catalog',
        loadComponent: () => import('@app/modules/admin/productos/producto-form.component').then(c => c.ProductoFormComponent)
      },
      {
        path: 'productos/:id', title: 'Producto · Agorá Catalog',
        loadComponent: () => import('@app/modules/admin/productos/producto-form.component').then(c => c.ProductoFormComponent)
      },
      {
        path: 'categorias', title: 'Categorías · Agorá Catalog',
        loadComponent: () => import('@app/modules/admin/categorias/categorias.component').then(c => c.CategoriasComponent)
      },
      {
        path: 'colecciones', title: 'Colecciones · Agorá Catalog',
        loadComponent: () => import('@app/modules/admin/colecciones/colecciones.component').then(c => c.ColeccionesComponent)
      },
      {
        path: 'configuracion', title: 'Configuración · Agorá Catalog',
        loadComponent: () => import('@app/modules/admin/configuracion/configuracion.component').then(c => c.ConfiguracionComponent)
      },
    ]
  },
  {
    // Catálogo público: /c/:publicId, /c/:publicId/catalogo y /c/:publicId/contacto (un solo componente).
    path: 'c/:publicId',
    loadComponent: () => import('@app/modules/catalogo/catalogo-shell.component').then(c => c.CatalogoShellComponent),
    children: [
      {path: '', children: []},
      {path: 'catalogo', children: []},
      {path: 'contacto', children: []},
    ]
  },
  {
    path: '**',
    loadComponent: () => import('@app/shared/components/not-found/not-found.component')
      .then(c => c.NotFoundComponent)
  }
];
