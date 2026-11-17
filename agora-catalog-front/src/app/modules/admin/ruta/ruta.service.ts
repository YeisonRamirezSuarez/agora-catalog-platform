import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';

import {Pagina, UnidadVenta} from '../admin.service';

/** Venta en ruta: tiendas que se visitan y carros con su stock (GET/POST /admin/tiendas y /admin/carros). */

export interface Tienda {
  id: number;
  nombre: string;
  encargado: string | null;
  telefono: string | null;
  direccion: string | null;
  barrio: string | null;
  notas: string | null;
  activa: boolean;
  pedidos: number;
}

export interface TiendaForm {
  nombre: string;
  encargado: string;
  telefono: string;
  direccion: string;
  barrio: string;
  notas: string;
  activa: boolean;
}

export interface FiltroTiendas {
  q: string;
  activa: boolean | null;
  page: number;
}

export interface Carro {
  id: number;
  nombre: string;
  placa: string | null;
  activo: boolean;
  /** Productos distintos que lleva. */
  productos: number;
  /** Lo que vale lo que lleva, a precio de catálogo. */
  valor: number;
}

export interface StockItem {
  productoId: number;
  codigo: string;
  nombre: string;
  presentacion: string | null;
  unidadVenta: UnidadVenta;
  cantidad: number;
  precio: number;
  imagenThumbUrl: string | null;
}

export interface CarroDetalle extends Carro {
  stock: StockItem[];
}

export interface CarroForm {
  nombre: string;
  placa: string;
  activo: boolean;
}

export interface LineaStock {
  productoId: number;
  /** null: la presentación principal del producto. */
  presentacion: string | null;
  cantidad: number;
}

/** Un producto en una ruta: traía de la anterior + cargó − vendió − quitó = queda. */
export interface RutaItem {
  productoId: number | null;
  nombre: string;
  presentacion: string | null;
  unidadVenta: UnidadVenta;
  traia: number;
  cargado: number;
  vendido: number;
  quitado: number;
  queda: number;
}

/** Una salida del carro. numero: 1.ª, 2.ª... ruta de ese día. cerradaAt null: es la que va. */
export interface RutaCarro {
  id: number;
  numero: number;
  iniciadaAt: string;
  cerradaAt: string | null;
  items: RutaItem[];
  /** Los pedidos que se hicieron en esta ruta, del primero al último. */
  pedidos: PedidoRuta[];
}

export interface PedidoRuta {
  id: number;
  numero: number;
  tiendaNombre: string;
  total: number;
  estado: 'CONFIRMADO' | 'ANULADO';
  createdAt: string;
}

export interface LineaPedido {
  productoId: number;
  presentacion: string | null;
  cantidad: number;
  /** null: se cobra el precio sugerido (el del catálogo). */
  precioUnitario: number | null;
}

/** La tienda va por id (elegida de la lista) o por nombre (escrita: se crea sola si no existe). */
export interface PedidoRequest {
  carroId: number;
  tiendaId: number | null;
  tiendaNombre: string | null;
  items: LineaPedido[];
  notas: string | null;
}

export interface ItemPedido {
  productoId: number | null;
  productoNombre: string;
  presentacion: string | null;
  unidadVenta: UnidadVenta;
  cantidad: number;
  precioSugerido: number;
  precioUnitario: number;
  subtotal: number;
}

export interface Pedido {
  id: number;
  numero: number;
  carroId: number;
  carroNombre: string;
  /** El carro se eliminó después: se nombra pero no se enlaza. */
  carroEliminado: boolean;
  tiendaId: number;
  tiendaNombre: string;
  tiendaTelefono: string | null;
  tiendaDireccion: string | null;
  formaPago: 'CONTADO' | 'CREDITO';
  total: number;
  estado: 'CONFIRMADO' | 'ANULADO';
  notas: string | null;
  usuarioNombre: string | null;
  createdAt: string;
  /** null si nunca se editó. */
  editadoAt: string | null;
  items: ItemPedido[];
}

/** N° 0012 */
export function numeroPedido(n: number): string {
  return 'N° ' + String(n).padStart(4, '0');
}

/** Abreviatura de la unidad para el comprobante: una presentación se cuenta en unidades. */
export function abrevUnidad(unidad: UnidadVenta, presentacion: string | null): string {
  if (presentacion) return 'und';
  return {UNIDAD: 'und', LIBRA: 'lb', KILO: 'kg', GRAMOS: 'g', PAQUETE: 'paq', CAJA: 'caja'}[unidad];
}

/** 3:30 p. m. */
export function hora(iso: string): string {
  return new Date(iso).toLocaleTimeString('es-CO', {hour: 'numeric', minute: '2-digit'});
}

/** «Hoy», «Ayer» o «Lunes 9 de noviembre». */
export function dia(iso: string): string {
  const d = new Date(iso), hoy = new Date(), ayer = new Date();
  ayer.setDate(hoy.getDate() - 1);
  if (d.toDateString() === hoy.toDateString()) return 'Hoy';
  if (d.toDateString() === ayer.toDateString()) return 'Ayer';
  const texto = d.toLocaleDateString('es-CO', {weekday: 'long', day: 'numeric', month: 'long'}).replace(',', '');
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

/** Unidades que se pueden cargar en fracciones (solo en la presentación principal). */
export const FRACCIONABLES: UnidadVenta[] = ['LIBRA', 'KILO'];

/** 2,5 · 10 · 0,125: hasta tres decimales, con coma. */
export function cantidad(n: number): string {
  return n.toLocaleString('es-CO', {maximumFractionDigits: 3});
}

/** 16 nov, 3:30 p. m. */
export function fechaHora(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleDateString('es-CO', {day: 'numeric', month: 'short'}).replace('.', '')
    + ', ' + d.toLocaleTimeString('es-CO', {hour: 'numeric', minute: '2-digit'});
}

/** Lee "2,5" o "2.5" como 2.5; vacío o inválido da null. */
export function leerCantidad(texto: string): number | null {
  const limpio = texto.trim().replace(',', '.');
  if (!/^\d+(\.\d+)?$/.test(limpio)) return null;
  return Number(limpio);
}

@Injectable({providedIn: 'root'})
export class RutaService {

  private readonly http = inject(HttpClient);

  tiendas(f: FiltroTiendas, size = 20): Observable<Pagina<Tienda>> {
    let params = new HttpParams().set('page', f.page).set('size', size);
    if (f.q.trim()) params = params.set('q', f.q.trim());
    if (f.activa !== null) params = params.set('activa', f.activa);
    return this.http.get<Pagina<Tienda>>('/admin/tiendas', {params});
  }

  crearTienda(t: TiendaForm): Observable<Tienda> {
    return this.http.post<Tienda>('/admin/tiendas', t);
  }

  actualizarTienda(id: number, t: TiendaForm): Observable<Tienda> {
    return this.http.put<Tienda>(`/admin/tiendas/${id}`, t);
  }

  setTiendaActiva(id: number, activo: boolean): Observable<Tienda> {
    return this.http.patch<Tienda>(`/admin/tiendas/${id}/activa`, {activo});
  }

  eliminarTienda(id: number): Observable<void> {
    return this.http.delete<void>(`/admin/tiendas/${id}`);
  }

  carros(): Observable<Carro[]> {
    return this.http.get<Carro[]>('/admin/carros');
  }

  carro(id: number): Observable<CarroDetalle> {
    return this.http.get<CarroDetalle>(`/admin/carros/${id}`);
  }

  crearCarro(c: CarroForm): Observable<CarroDetalle> {
    return this.http.post<CarroDetalle>('/admin/carros', c);
  }

  actualizarCarro(id: number, c: CarroForm): Observable<CarroDetalle> {
    return this.http.put<CarroDetalle>(`/admin/carros/${id}`, c);
  }

  setCarroActivo(id: number, activo: boolean): Observable<Carro> {
    return this.http.patch<Carro>(`/admin/carros/${id}/activo`, {activo});
  }

  eliminarCarro(id: number): Observable<void> {
    return this.http.delete<void>(`/admin/carros/${id}`);
  }

  cargar(id: number, items: LineaStock[], nota: string): Observable<CarroDetalle> {
    return this.http.post<CarroDetalle>(`/admin/carros/${id}/cargas`, {items, nota: nota.trim() || null});
  }

  ajustar(id: number, items: LineaStock[], motivo: string): Observable<CarroDetalle> {
    return this.http.post<CarroDetalle>(`/admin/carros/${id}/ajustes`, {items, motivo});
  }

  descargar(id: number, nota: string): Observable<CarroDetalle> {
    return this.http.post<CarroDetalle>(`/admin/carros/${id}/descargue`, {nota: nota.trim() || null});
  }

  /** Cierra la ruta que va y empieza otra; lo que sobró sigue en el carro o se baja. */
  nuevaRuta(id: number, llevarSobrante: boolean): Observable<CarroDetalle> {
    return this.http.post<CarroDetalle>(`/admin/carros/${id}/rutas`, {llevarSobrante});
  }

  rutas(id: number, page: number, size = 10): Observable<Pagina<RutaCarro>> {
    return this.http.get<Pagina<RutaCarro>>(`/admin/carros/${id}/rutas`, {params: {page, size}});
  }

  crearPedido(p: PedidoRequest): Observable<Pedido> {
    return this.http.post<Pedido>('/admin/pedidos-tienda', p);
  }

  /** Corrige un pedido: las líneas nuevas reemplazan a las anteriores y el carro se reacomoda. */
  editarPedido(id: number, p: Omit<PedidoRequest, 'carroId'>): Observable<Pedido> {
    return this.http.put<Pedido>(`/admin/pedidos-tienda/${id}`, p);
  }

  pedido(id: number): Observable<Pedido> {
    return this.http.get<Pedido>(`/admin/pedidos-tienda/${id}`);
  }
}
