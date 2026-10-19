import {Filtros, Horario, Negocio, Producto, UnidadVenta} from './catalogo.models';

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

/**
 * Mensaje de WhatsApp del pedido; sin precios visibles no lleva montos. {@code envio}: sin él, no hay
 * línea de domicilio (recoge en la tienda o el negocio no configuró domicilio); null, el negocio
 * confirma el costo; 0, gratis.
 */
export function mensajePedido(lineasPedido: LineaPedido[], precios: boolean,
                              datos: {nombre?: string; entrega?: string; envio?: number | null} = {}): string {
  const total = lineasPedido.reduce((s, l) => s + efectivo(l.producto) * l.cantidad, 0);
  const {nombre, entrega, envio} = datos, conEnvio = precios && envio !== undefined;
  return [
    'Hola, quiero hacer este pedido:',
    ...lineasPedido.map(({producto: p, cantidad: n}) =>
      `• ${n} × ${p.nombre}${p.presentacion ? ` (${p.presentacion})` : ''}${precios ? `: ${cop(efectivo(p) * n)}` : ''}`),
    conEnvio && envio !== null ? `Subtotal: ${cop(total)}` : '',
    conEnvio ? `Domicilio: ${envio === null ? 'por confirmar' : envio ? cop(envio) : 'gratis'}` : '',
    precios ? `Total: ${cop(total + (envio ?? 0))}` : '',
    nombre ? `Nombre: ${nombre}` : '',
    entrega ? `Entrega: ${entrega}` : ''
  ].filter(Boolean).join('\n');
}

/** Costo del domicilio para este subtotal: null si lo confirma el negocio, 0 si es gratis. */
export function costoDomicilio(n: Negocio, subtotal: number): number | null {
  return n.domicilioGratisDesde && subtotal >= n.domicilioGratisDesde ? 0 : n.domicilioCosto;
}

/** "Domicilio $3.000 · gratis desde $50.000 · pedido mínimo $20.000" para la sección de contacto. */
export function resumenDomicilio(n: Negocio, precios: boolean): string {
  if (!n.domicilioActivo || !precios) return '';
  const t = [
    n.domicilioCosto === null ? '' : n.domicilioCosto ? `domicilio ${cop(n.domicilioCosto)}` : 'domicilio gratis',
    n.domicilioCosto && n.domicilioGratisDesde ? `gratis desde ${cop(n.domicilioGratisDesde)}` : '',
    n.domicilioMinimo ? `pedido mínimo ${cop(n.domicilioMinimo)}` : ''
  ].filter(Boolean).join(' · ');
  return t && t[0].toUpperCase() + t.slice(1);
}

const DIAS = ['lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado', 'domingo'];

/** "07:00" -> "7:00 a. m."; "19:30" -> "7:30 p. m." */
export function hora(hhmm: string): string {
  const [h, m] = hhmm.split(':');
  return `${+h % 12 || 12}:${m} ${+h < 12 ? 'a. m.' : 'p. m.'}`;
}

/** Día (1 = lunes) y "HH:mm" de este momento en la zona del negocio. */
function enZona(ahora: Date, zona: string): {dia: number; hhmm: string} {
  const p = Object.fromEntries(new Intl.DateTimeFormat('en-US',
    {timeZone: zona, weekday: 'short', hour: '2-digit', minute: '2-digit', hourCycle: 'h23'})
    .formatToParts(ahora).map(x => [x.type, x.value]));
  return {dia: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].indexOf(p['weekday']) + 1, hhmm: `${p['hour']}:${p['minute']}`};
}

/** Abierto ("cierra 7:00 p. m.") o cerrado ("abre mañana 7:00 a. m."); null si el negocio no puso horario. */
export function estadoHorario(semana: Horario[], zona: string, ahora: Date): {abierto: boolean; texto: string} | null {
  if (!semana.length) return null;
  const {dia, hhmm} = enZona(ahora, zona);
  const hoy = semana.find(h => h.dia === dia);
  if (hoy && hoy.abre <= hhmm && hhmm < hoy.cierra) return {abierto: true, texto: `cierra ${hora(hoy.cierra)}`};
  // El próximo día que abre; i = 7 es el mismo día de la semana que viene.
  for (let i = 0; i <= 7; i++) {
    const d = (dia - 1 + i) % 7 + 1, h = semana.find(x => x.dia === d);
    if (h && (i > 0 || hhmm < h.abre)) {
      return {abierto: false, texto: `abre ${i === 0 ? 'hoy' : i === 1 ? 'mañana' : 'el ' + DIAS[d - 1]} ${hora(h.abre)}`};
    }
  }
  return null;
}

/** "Lunes a sábado: 7:00 a. m. – 7:00 p. m.", juntando los días seguidos con el mismo horario. */
export function resumenHorario(semana: Horario[]): string[] {
  const grupos: {desde: number; hasta: number; h: Horario}[] = [];
  for (const h of [...semana].sort((a, b) => a.dia - b.dia)) {
    const g = grupos.at(-1);
    if (g && g.hasta === h.dia - 1 && g.h.abre === h.abre && g.h.cierra === h.cierra) g.hasta = h.dia;
    else grupos.push({desde: h.dia, hasta: h.dia, h});
  }
  return grupos.map(({desde, hasta, h}) => {
    const dias = desde === hasta ? DIAS[desde - 1] : `${DIAS[desde - 1]} ${hasta - desde === 1 ? 'y' : 'a'} ${DIAS[hasta - 1]}`;
    return `${dias[0].toUpperCase()}${dias.slice(1)}: ${hora(h.abre)} – ${hora(h.cierra)}`;
  });
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
