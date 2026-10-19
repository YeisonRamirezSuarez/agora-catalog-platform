import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';

import {PanelLink} from '@app/shared/layouts/panel-shell/panel-shell.component';

/** Menú del panel del negocio, el mismo en todas sus pantallas. */
export const ADMIN_LINKS: PanelLink[] = [
  {label: 'Inicio', path: '/admin'},
  {label: 'Productos', path: '/admin/productos'},
  {label: 'Categorías', path: '/admin/categorias'},
  {label: 'Colecciones', path: '/admin/colecciones'},
  {label: 'Importar', path: '/admin/importar'},
  {label: 'Configuración', path: '/admin/configuracion'},
];

export type UnidadVenta = 'UNIDAD' | 'LIBRA' | 'KILO' | 'GRAMOS' | 'PAQUETE' | 'CAJA';

export const UNIDADES: {valor: UnidadVenta; texto: string}[] = [
  {valor: 'UNIDAD', texto: 'unidad'}, {valor: 'LIBRA', texto: 'libra'}, {valor: 'KILO', texto: 'kilo'},
  {valor: 'GRAMOS', texto: 'gramos'}, {valor: 'PAQUETE', texto: 'paquete'}, {valor: 'CAJA', texto: 'caja'},
];

export interface Pagina<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface Dashboard {
  productos: number;
  sinFoto: number;
  agotados: number;
  ofertasVigentes: number;
  linkPublico: string;
}

/** Contadores del catálogo público (GET /admin/estadisticas). */
export interface Estadisticas {
  dias: number;
  visitas: number;
  pedidos: number;
  whatsapp: number;
  porDia: {fecha: string; visitas: number; productosVistos: number; pedidos: number; whatsapp: number}[];
  masVistos: {id: number; nombre: string; imagenThumbUrl: string | null; vistas: number}[];
}

export interface Categoria {
  id: number;
  nombre: string;
  orden: number;
  activa: boolean;
  imagenUrl: string | null;
  productos: number;
  bannerActivo: boolean;
  bannerUrl: string | null;
  bannerTitulo: string | null;
  bannerFrase: string | null;
  bannerNota: string | null;
}

export interface Producto {
  id: number;
  codigo: string;
  categoriaId: number;
  categoriaNombre: string;
  nombre: string;
  descripcion: string | null;
  presentacion: string | null;
  unidadVenta: UnidadVenta;
  precio: number;
  precioOferta: number | null;
  ofertaDesde: string | null;
  ofertaHasta: string | null;
  ofertaVigente: boolean;
  imagenUrl: string | null;
  imagenThumbUrl: string | null;
  disponible: boolean;
  destacado: boolean;
  orden: number;
}

export interface ProductoForm {
  codigo: string;
  categoriaId: number | null;
  nombre: string;
  descripcion: string;
  presentacion: string;
  unidadVenta: UnidadVenta;
  precio: number | null;
  precioOferta: number | null;
  ofertaDesde: string | null;
  ofertaHasta: string | null;
  disponible: boolean;
  destacado: boolean;
}

export type Fachada = 'SALSAMENTARIA' | 'PANADERIA' | 'FRUTERIA' | 'CAFETERIA' | 'MINIMERCADO' | 'FERRETERIA';
export type IconoSello = 'CALIDAD' | 'DOMICILIO' | 'WHATSAPP' | 'PRECIO' | 'TRADICION' | 'HORARIO';

export interface Recorte {
  x: number;
  y: number;
  ancho: number;
  alto: number;
}

/** Configuración del negocio (GET/PUT /admin/empresa). */
export interface Configuracion {
  id: number;
  nombre: string;
  slug: string;
  publicHash: string;
  linkPublico: string;
  nombreCorto: string | null;
  lema: string | null;
  logoUrl: string | null;
  portadaUrl: string | null;
  logoOriginalUrl: string | null;
  logoRecorte: Recorte | null;
  portadaOriginalUrl: string | null;
  portadaRecorte: Recorte | null;
  portadaAntetitulo: string | null;
  portadaTitulo: string | null;
  portadaTituloAcento: string | null;
  portadaSubtitulo: string | null;
  portadaNota: string | null;
  portadaSelloTexto: string | null;
  catalogoTitulo: string | null;
  catalogoAcento: string | null;
  catalogoSubtitulo: string | null;
  coleccionesTitulo: string;
  coleccionesTexto: string | null;
  coleccionesNota: string | null;
  visitaNota: string | null;
  fachada: Fachada | null;
  adornos: boolean;
  direccion: string | null;
  horario: string | null;
  infoDomicilios: string | null;
  mostrarPrecios: boolean;
  pedidosWhatsapp: boolean;
  mostrarAgotados: boolean;
  colorPrimario: string;
  colorSecundario: string;
  whatsapps: {numero: string; nombre: string | null}[];
  sellos: {icono: IconoSello; texto: string}[];
}

export interface Coleccion {
  id: number;
  nombre: string;
  descripcion: string | null;
  imagenUrl: string | null;
  visible: boolean;
  orden: number;
  productos: number[];
}

/** Resultado de revisar o aplicar un Excel (`fila` es la del Excel; la 1 es la de títulos). */
export interface ReporteImportacion {
  filas: number;
  nuevos: number;
  actualizados: number;
  categoriasNuevas: string[];
  errores: {fila: number; mensaje: string}[];
  aplicado: boolean;
}

export interface ReporteImagenes {
  asignadas: string[];
  sinProducto: string[];
  conError: {archivo: string; mensaje: string}[];
}

export interface FiltroProductos {
  q: string;
  categoria: number | null;
  disponible: boolean | null;
  page: number;
}

/** API del panel del negocio (/admin/**). La empresa sale del token: nunca se manda. */
@Injectable({providedIn: 'root'})
export class AdminService {

  private readonly http = inject(HttpClient);

  dashboard(): Observable<Dashboard> {
    return this.http.get<Dashboard>('/admin/dashboard');
  }

  estadisticas(dias = 30): Observable<Estadisticas> {
    return this.http.get<Estadisticas>('/admin/estadisticas', {params: {dias}});
  }

  // ---------- Categorías ----------

  categorias(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>('/admin/categorias');
  }

  crearCategoria(nombre: string): Observable<Categoria> {
    return this.http.post<Categoria>('/admin/categorias', {nombre, activa: true});
  }

  actualizarCategoria(c: Categoria): Observable<Categoria> {
    return this.http.put<Categoria>(`/admin/categorias/${c.id}`, {nombre: c.nombre, activa: c.activa});
  }

  eliminarCategoria(id: number): Observable<void> {
    return this.http.delete<void>(`/admin/categorias/${id}`);
  }

  ordenarCategorias(ids: number[]): Observable<Categoria[]> {
    return this.http.put<Categoria[]>('/admin/categorias/orden', {ids});
  }

  guardarFranja(id: number, datos: {titulo: string; frase: string; nota: string}, archivo: File | null): Observable<Categoria> {
    const form = new FormData();
    form.set('titulo', datos.titulo);
    form.set('frase', datos.frase);
    form.set('nota', datos.nota);
    if (archivo) form.set('archivo', archivo);
    return this.http.put<Categoria>(`/admin/categorias/${id}/franja`, form);
  }

  quitarFranja(id: number): Observable<Categoria> {
    return this.http.delete<Categoria>(`/admin/categorias/${id}/franja`);
  }

  // ---------- Productos ----------

  productos(f: FiltroProductos, size = 20): Observable<Pagina<Producto>> {
    let params = new HttpParams().set('page', f.page).set('size', size);
    if (f.q.trim()) params = params.set('q', f.q.trim());
    if (f.categoria) params = params.set('categoria', f.categoria);
    if (f.disponible !== null) params = params.set('disponible', f.disponible);
    return this.http.get<Pagina<Producto>>('/admin/productos', {params});
  }

  producto(id: number): Observable<Producto> {
    return this.http.get<Producto>(`/admin/productos/${id}`);
  }

  crearProducto(p: ProductoForm): Observable<Producto> {
    return this.http.post<Producto>('/admin/productos', p);
  }

  actualizarProducto(id: number, p: ProductoForm): Observable<Producto> {
    return this.http.put<Producto>(`/admin/productos/${id}`, p);
  }

  eliminarProducto(id: number): Observable<void> {
    return this.http.delete<void>(`/admin/productos/${id}`);
  }

  setDisponible(id: number, disponible: boolean): Observable<Producto> {
    return this.http.patch<Producto>(`/admin/productos/${id}/disponible`, {disponible});
  }

  cambiarPrecios(cambios: {id: number; precio: number}[]): Observable<void> {
    return this.http.patch<void>('/admin/productos/precios', {cambios});
  }

  subirFoto(id: number, archivo: File): Observable<Producto> {
    const form = new FormData();
    form.set('archivo', archivo);
    return this.http.post<Producto>(`/admin/productos/${id}/imagen`, form);
  }

  quitarFoto(id: number): Observable<Producto> {
    return this.http.delete<Producto>(`/admin/productos/${id}/imagen`);
  }

  /** Todos los productos (para elegir los de una colección), página por página. */
  todosLosProductos(): Observable<Producto[]> {
    return new Observable<Producto[]>(sub => {
      const todos: Producto[] = [];
      const pedir = (page: number) => this.productos({q: '', categoria: null, disponible: null, page}, 100).subscribe({
        next: pg => {
          todos.push(...pg.content);
          if (page + 1 < pg.totalPages) pedir(page + 1); else { sub.next(todos); sub.complete(); }
        },
        error: e => sub.error(e)
      });
      pedir(0);
    });
  }

  // ---------- Configuración ----------

  configuracion(): Observable<Configuracion> {
    return this.http.get<Configuracion>('/admin/empresa');
  }

  guardarConfiguracion(c: Configuracion): Observable<Configuracion> {
    return this.http.put<Configuracion>('/admin/empresa', c);
  }

  /** Logo o portada: archivo nuevo (opcional) y el recorte en píxeles del original. */
  guardarImagenEmpresa(tipo: 'logo' | 'portada', archivo: File | null, r: Recorte): Observable<Configuracion> {
    const form = new FormData();
    if (archivo) form.set('archivo', archivo);
    form.set('x', String(r.x));
    form.set('y', String(r.y));
    form.set('ancho', String(r.ancho));
    form.set('alto', String(r.alto));
    return this.http.post<Configuracion>(`/admin/empresa/${tipo}`, form);
  }

  quitarImagenEmpresa(tipo: 'logo' | 'portada'): Observable<Configuracion> {
    return this.http.delete<Configuracion>(`/admin/empresa/${tipo}`);
  }

  // ---------- Colecciones ----------

  colecciones(): Observable<Coleccion[]> {
    return this.http.get<Coleccion[]>('/admin/colecciones');
  }

  guardarColeccion(c: {id?: number; nombre: string; descripcion: string; visible: boolean; productos: number[]}): Observable<Coleccion> {
    const body = {nombre: c.nombre, descripcion: c.descripcion, visible: c.visible, productos: c.productos};
    return c.id ? this.http.put<Coleccion>(`/admin/colecciones/${c.id}`, body) : this.http.post<Coleccion>('/admin/colecciones', body);
  }

  eliminarColeccion(id: number): Observable<void> {
    return this.http.delete<void>(`/admin/colecciones/${id}`);
  }

  ordenarColecciones(ids: number[]): Observable<Coleccion[]> {
    return this.http.put<Coleccion[]>('/admin/colecciones/orden', {ids});
  }

  fotoColeccion(id: number, archivo: File): Observable<Coleccion> {
    const form = new FormData();
    form.set('archivo', archivo);
    return this.http.post<Coleccion>(`/admin/colecciones/${id}/imagen`, form);
  }

  // ---------- Importación ----------

  /** Plantilla o exportación: va con el token, por eso se baja como blob y no con un link. */
  descargarExcel(cual: 'plantilla' | 'exportar'): Observable<Blob> {
    return this.http.get(`/admin/importacion/${cual}`, {responseType: 'blob'});
  }

  importarExcel(paso: 'previsualizar' | 'confirmar', archivo: File): Observable<ReporteImportacion> {
    const form = new FormData();
    form.set('archivo', archivo);
    return this.http.post<ReporteImportacion>(`/admin/importacion/${paso}`, form);
  }

  fotosZip(archivo: File): Observable<ReporteImagenes> {
    const form = new FormData();
    form.set('archivo', archivo);
    return this.http.post<ReporteImagenes>('/admin/importacion/imagenes', form);
  }

  guardarSeccionColecciones(s: {titulo: string; texto: string; nota: string}): Observable<Configuracion> {
    return this.http.put<Configuracion>('/admin/colecciones/seccion', s);
  }
}

/** Contraste WCAG entre dos colores hex: el catálogo exige 4,5:1 (igual que el backend). */
export function contraste(a: string, b: string): number {
  const lum = (hex: string) => {
    const n = parseInt(hex.slice(1), 16);
    const c = (v: number) => { const s = v / 255; return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4); };
    return 0.2126 * c((n >> 16) & 255) + 0.7152 * c((n >> 8) & 255) + 0.0722 * c(n & 255);
  };
  const [x, y] = [lum(a), lum(b)];
  return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05);
}

/** Pesos colombianos sin decimales: 12500 -> "$12.500". */
export const cop = (n: number): string => '$' + Math.round(n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
