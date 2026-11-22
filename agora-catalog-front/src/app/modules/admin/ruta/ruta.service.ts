import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';

import {Pagina, UnidadVenta} from '../admin.service';

/** Venta en ruta: carros con su stock, pedidos a tiendas y el tablero de rutas. */

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

/** La tienda es solo un nombre: queda en el pedido y no se guarda en ningún otro lado. */
export interface PedidoRequest {
  carroId: number;
  tiendaNombre: string;
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
  tiendaNombre: string;
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

/** Una ruta en el tablero del día. */
export interface RutaDia {
  id: number;
  carroId: number;
  carroNombre: string;
  carroEliminado: boolean;
  numero: number;
  iniciadaAt: string;
  cerradaAt: string | null;
  pedidos: number;
  vendido: number;
}

export interface ProductoVendido {
  nombre: string;
  presentacion: string | null;
  unidadVenta: UnidadVenta;
  cantidad: number;
  total: number;
}

export interface PedidoDia {
  id: number;
  numero: number;
  tiendaNombre: string;
  carroNombre: string;
  total: number;
  estado: 'CONFIRMADO' | 'ANULADO';
  createdAt: string;
}

/** Cómo le fue a la venta en ruta un día (GET /admin/tablero-ruta). */
export interface Tablero {
  fecha: string;
  vendido: number;
  pedidos: number;
  rutas: RutaDia[];
  productos: ProductoVendido[];
  pedidosDia: PedidoDia[];
}

/** El comprobante en texto, listo para WhatsApp. */
export function textoComprobante(p: Pedido, negocio: string, cop: (n: number) => string): string {
  const lineas = p.items.map(i => `• ${cantidad(i.cantidad)} ${abrevUnidad(i.unidadVenta, i.presentacion)} ${i.productoNombre}`
    + (i.presentacion ? ` (${i.presentacion})` : '') + ` — ${cop(i.subtotal)}`);
  return [
    `*${negocio}*`,
    `Comprobante de entrega ${numeroPedido(p.numero)}` + (p.editadoAt ? ' (corregido)' : ''),
    `${p.tiendaNombre} · ${fechaHora(p.createdAt)}`,
    '',
    ...lineas,
    '',
    `*Total: ${cop(p.total)}*`,
    ...(p.notas ? ['', `Notas: ${p.notas}`] : [])
  ].join('\n');
}

/**
 * Enlace de WhatsApp con el texto. Con número, abre el chat de ese número (a un celular colombiano de
 * 10 dígitos se le antepone el 57); sin número, WhatsApp deja escoger el contacto.
 */
export function enlaceWhatsapp(texto: string, telefono: string): string {
  const tel = telefono.replace(/\D/g, '');
  const numero = tel.length === 10 && tel.startsWith('3') ? '57' + tel : tel;
  return `https://wa.me/${numero}?text=${encodeURIComponent(texto)}`;
}

/** Un número que sirve para WhatsApp: vacío (se escoge el contacto) o de 10 a 15 dígitos. */
export function telefonoValido(telefono: string): boolean {
  const tel = telefono.replace(/\D/g, '');
  return !tel || (tel.length >= 10 && tel.length <= 15);
}

/** 2026-11-22 de una fecha, en la hora del teléfono. */
export function fechaIso(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
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

  /** Nombres de tienda ya usados en pedidos, para sugerirlos al escribir. */
  nombresTiendas(q: string): Observable<string[]> {
    return this.http.get<string[]>('/admin/pedidos-tienda/tiendas', {params: {q}});
  }

  /** El tablero de un día (2026-11-22); sin fecha, el de hoy. */
  tablero(fecha: string): Observable<Tablero> {
    return this.http.get<Tablero>('/admin/tablero-ruta', {params: {fecha}});
  }
}
