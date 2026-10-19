import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';

import {PanelLink} from '@app/shared/layouts/panel-shell/panel-shell.component';

/** Menú del panel del negocio, el mismo en todas sus pantallas. */
export const ADMIN_LINKS: PanelLink[] = [
  {label: 'Inicio', path: '/admin'},
  {label: 'Productos', path: '/admin/productos'},
  {label: 'Categorías', path: '/admin/categorias'},
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
}

/** Pesos colombianos sin decimales: 12500 -> "$12.500". */
export const cop = (n: number): string => '$' + Math.round(n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
