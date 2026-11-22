import {Routes} from '@angular/router';

import {invitadoGuard, moduloGuard, rolGuard, sesionGuard} from '@app/core/guards/auth.guard';

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
        path: 'colecciones', canActivate: [moduloGuard('COLECCIONES')], title: 'Colecciones · Agorá Catalog',
        loadComponent: () => import('@app/modules/admin/colecciones/colecciones.component').then(c => c.ColeccionesComponent)
      },
      {
        path: 'pedidos/nuevo', canActivate: [moduloGuard('VENTA_RUTA')], title: 'Nuevo pedido · Agorá Catalog',
        loadComponent: () => import('@app/modules/admin/ruta/nuevo-pedido.component').then(c => c.NuevoPedidoComponent)
      },
      {
        path: 'pedidos/:id/editar', canActivate: [moduloGuard('VENTA_RUTA')], title: 'Editar pedido · Agorá Catalog',
        loadComponent: () => import('@app/modules/admin/ruta/nuevo-pedido.component').then(c => c.NuevoPedidoComponent)
      },
      {
        path: 'pedidos/:id', canActivate: [moduloGuard('VENTA_RUTA')], title: 'Pedido · Agorá Catalog',
        loadComponent: () => import('@app/modules/admin/ruta/comprobante.component').then(c => c.ComprobanteComponent)
      },
      {
        path: 'carros', canActivate: [moduloGuard('VENTA_RUTA')], title: 'Carros · Agorá Catalog',
        loadComponent: () => import('@app/modules/admin/ruta/carros.component').then(c => c.CarrosComponent)
      },
      {
        path: 'carros/:id', canActivate: [moduloGuard('VENTA_RUTA')], title: 'Carro · Agorá Catalog',
        loadComponent: () => import('@app/modules/admin/ruta/carro-detalle.component').then(c => c.CarroDetalleComponent)
      },
      {
        path: 'rutas', canActivate: [moduloGuard('VENTA_RUTA')], title: 'Rutas · Agorá Catalog',
        loadComponent: () => import('@app/modules/admin/ruta/tablero-rutas.component').then(c => c.TableroRutasComponent)
      },
      {
        path: 'importar', canActivate: [moduloGuard('IMPORTAR_EXCEL')], title: 'Importar · Agorá Catalog',
        loadComponent: () => import('@app/modules/admin/importar/importar.component').then(c => c.ImportarComponent)
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
