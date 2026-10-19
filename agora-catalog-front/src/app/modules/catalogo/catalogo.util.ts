import {Filtros, Producto, UnidadVenta} from './catalogo.models';

export const UNIDADES: Record<UnidadVenta, string> = {
  UNIDAD: 'unidad', LIBRA: 'libra', KILO: 'kilo', GRAMOS: 'gramos', PAQUETE: 'paquete', CAJA: 'caja'
};

/** Pesos colombianos sin decimales: 12500 -> "$12.500". */
export const cop = (n: number): string => '$' + Math.round(n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');

export const plural = (n: number, palabra: string): string => `${n} ${palabra}${n === 1 ? '' : 's'}`;

/** Para buscar sin tildes ni mayúsculas. */
export const norm = (s: string): string => s.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');

/** 573163218276 -> "+57 316 321 8276". */
export const phone = (n: string): string => n.replace(/^(\d{2})(\d{3})(\d{3})(\d+)$/, '+$1 $2 $3 $4');

/** El dueño parte las líneas de los titulares con Enter. */
export const lineas = (s: string | null | undefined): string[] => (s ?? '').split('\n');

export const MSG_GENERAL = 'Hola, vi su catálogo y quiero hacer un pedido.';
export const msgProducto = (p: Producto): string =>
  `Hola, vi en su catálogo: ${p.nombre}${p.presentacion ? ` (${p.presentacion})` : ''}. ¿Lo tiene disponible?`;
export const waHref = (numero: string, mensaje = MSG_GENERAL): string =>
  `https://wa.me/${numero}?text=${encodeURIComponent(mensaje)}`;

/** Precio que paga el visitante: el de oferta si está vigente. */
export const efectivo = (p: Producto): number => p.precioFinal ?? p.precio ?? 0;

/** Orden "Más relevantes": disponibles primero, luego destacados y ofertas, sin perder el orden del negocio. */
export function porRelevancia(lista: Producto[]): Producto[] {
  const peso = (p: Producto) => (p.disponible ? 0 : 4) + (p.destacado ? 0 : 1) + (p.ofertaVigente ? 0 : 1);
  return lista.map((p, i) => [p, i] as const).sort((a, b) => peso(a[0]) - peso(b[0]) || a[1] - b[1]).map(x => x[0]);
}

export interface LineaPedido {
  producto: Producto;
  cantidad: number;
}

/** Mensaje de WhatsApp del pedido; sin precios visibles no lleva montos. */
export function mensajePedido(lineasPedido: LineaPedido[], precios: boolean, nombre = '', entrega = ''): string {
  const total = lineasPedido.reduce((s, l) => s + efectivo(l.producto) * l.cantidad, 0);
  return [
    'Hola, quiero hacer este pedido:',
    ...lineasPedido.map(({producto: p, cantidad: n}) =>
      `• ${n} × ${p.nombre}${p.presentacion ? ` (${p.presentacion})` : ''}${precios ? `: ${cop(efectivo(p) * n)}` : ''}`),
    precios ? `Total: ${cop(total)}` : '',
    nombre ? `Nombre: ${nombre}` : '',
    entrega ? `Entrega: ${entrega}` : ''
  ].filter(Boolean).join('\n');
}

export const FILTROS_VACIOS: Filtros = {
  cats: [], promo: false, mas: false, disp: false, favs: false, col: 0, q: '', min: 0, max: 0, orden: 'rel', lista: false
};

/** ?cat=1,3&promo=1&q=queso&orden=menor -> Filtros */
export function leerFiltros(q: Record<string, string | undefined>): Filtros {
  const orden = q['orden'];
  return {
    cats: (q['cat'] ?? '').split(',').filter(Boolean).map(Number).filter(n => n > 0),
    promo: q['promo'] === '1', mas: q['mas'] === '1', disp: q['disp'] === '1', favs: q['fav'] === '1',
    col: Number(q['col']) || 0, q: q['q'] ?? '', min: Number(q['min']) || 0, max: Number(q['max']) || 0,
    orden: orden === 'menor' || orden === 'mayor' || orden === 'az' ? orden : 'rel',
    lista: q['vista'] === 'lista'
  };
}

/** Filtros -> query params (solo los que tienen valor, para que el link sea corto). */
export function filtrosAQuery(f: Filtros): Record<string, string> {
  const q: Record<string, string> = {};
  if (f.cats.length) q['cat'] = f.cats.join(',');
  if (f.promo) q['promo'] = '1';
  if (f.mas) q['mas'] = '1';
  if (f.disp) q['disp'] = '1';
  if (f.favs) q['fav'] = '1';
  if (f.col) q['col'] = String(f.col);
  if (f.q) q['q'] = f.q;
  if (f.min) q['min'] = String(f.min);
  if (f.max) q['max'] = String(f.max);
  if (f.orden !== 'rel') q['orden'] = f.orden;
  if (f.lista) q['vista'] = 'lista';
  return q;
}
