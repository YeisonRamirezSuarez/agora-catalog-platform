import {Negocio, Producto} from './catalogo.models';
import {
  cop, costoDomicilio, estadoHorario, filtrosAQuery, leerFiltros, mensajePedido, norm, phone, resumenDomicilio, resumenHorario
} from './catalogo.util';

const queso = {
  id: 1, categoriaId: 3, nombre: 'Queso campesino', descripcion: null, presentacion: 'Bloque', unidadVenta: 'LIBRA',
  precio: 12000, precioFinal: 12000, ofertaVigente: false, imagenUrl: null, imagenThumbUrl: null,
  disponible: true, destacado: false, nuevo: false
} as Producto;

describe('catalogo.util', () => {

  it('formatea pesos, teléfonos y busca sin tildes', () => {
    expect(cop(24000)).toBe('$24.000');
    expect(cop(1250000)).toBe('$1.250.000');
    expect(phone('573163218276')).toBe('+57 316 321 8276');
    expect(norm('Lácteos')).toBe('lacteos');
  });

  it('arma el mensaje del pedido con o sin precios', () => {
    expect(mensajePedido([{producto: queso, cantidad: 2}], true, {nombre: 'Ana'}))
      .toBe('Hola, quiero hacer este pedido:\n• 2 × Queso campesino (Bloque): $24.000\nTotal: $24.000\nNombre: Ana');
    expect(mensajePedido([{producto: queso, cantidad: 2}], false))
      .toBe('Hola, quiero hacer este pedido:\n• 2 × Queso campesino (Bloque)');
  });

  it('suma el domicilio al total del mensaje', () => {
    const pedido = [{producto: queso, cantidad: 1}];
    expect(mensajePedido(pedido, true, {envio: 3000, entrega: 'domicilio a Calle 1'})).toBe('Hola, quiero hacer este pedido:\n'
      + '• 1 × Queso campesino (Bloque): $12.000\nSubtotal: $12.000\nDomicilio: $3.000\nTotal: $15.000\nEntrega: domicilio a Calle 1');
    expect(mensajePedido(pedido, true, {envio: 0})).toContain('Domicilio: gratis\nTotal: $12.000');
    expect(mensajePedido(pedido, true, {envio: null})).toContain('$12.000\nDomicilio: por confirmar\nTotal: $12.000');
    expect(mensajePedido(pedido, false, {envio: 3000})).not.toContain('Domicilio');
  });

  it('el domicilio es gratis desde el valor que puso el negocio', () => {
    const n = {domicilioActivo: true, domicilioCosto: 3000, domicilioMinimo: 20000, domicilioGratisDesde: 50000} as Negocio;
    expect(costoDomicilio(n, 49999)).toBe(3000);
    expect(costoDomicilio(n, 50000)).toBe(0);
    expect(costoDomicilio({...n, domicilioCosto: null, domicilioGratisDesde: null}, 90000)).toBeNull();
    expect(resumenDomicilio(n, true)).toBe('Domicilio $3.000 · gratis desde $50.000 · pedido mínimo $20.000');
    expect(resumenDomicilio({...n, domicilioCosto: null}, true)).toBe('Pedido mínimo $20.000');
    expect(resumenDomicilio(n, false)).toBe('');
  });

  it('dice si el negocio está abierto con su hora, no con la del visitante', () => {
    const semana = [1, 2, 3, 4, 5, 6].map(dia => ({dia, abre: '07:00', cierra: '19:00'}));
    const zona = 'America/Bogota';
    // 2026-10-19 es lunes; Bogotá es UTC-5.
    const en = (iso: string) => estadoHorario(semana, zona, new Date(iso));
    expect(en('2026-10-19T15:00:00Z')).toEqual({abierto: true, texto: 'cierra 7:00 p. m.'});
    expect(en('2026-10-19T11:59:00Z')).toEqual({abierto: false, texto: 'abre hoy 7:00 a. m.'});
    expect(en('2026-10-20T00:00:00Z')).toEqual({abierto: false, texto: 'abre mañana 7:00 a. m.'});
    // Sábado 7:00 p. m. en Bogotá: el domingo no abre.
    expect(en('2026-10-25T00:00:00Z')).toEqual({abierto: false, texto: 'abre el lunes 7:00 a. m.'});
    expect(estadoHorario([{dia: 1, abre: '08:00', cierra: '12:00'}], zona, new Date('2026-10-19T18:00:00Z')))
      .toEqual({abierto: false, texto: 'abre el lunes 8:00 a. m.'});
    expect(estadoHorario([], zona, new Date())).toBeNull();
  });

  it('resume el horario juntando los días iguales', () => {
    expect(resumenHorario([
      ...[1, 2, 3, 4, 5, 6].map(dia => ({dia, abre: '07:00', cierra: '19:00'})), {dia: 7, abre: '08:00', cierra: '13:00'}
    ])).toEqual(['Lunes a sábado: 7:00 a. m. – 7:00 p. m.', 'Domingo: 8:00 a. m. – 1:00 p. m.']);
    expect(resumenHorario([{dia: 6, abre: '09:00', cierra: '12:30'}, {dia: 7, abre: '09:00', cierra: '12:30'}]))
      .toEqual(['Sábado y domingo: 9:00 a. m. – 12:30 p. m.']);
  });

  it('los filtros van y vuelven de la URL', () => {
    const f = leerFiltros({cat: '1,3', promo: '1', q: 'queso', orden: 'menor'});
    expect(f.cats).toEqual([1, 3]);
    expect(filtrosAQuery(f)).toEqual({cat: '1,3', promo: '1', q: 'queso', orden: 'menor'});
    expect(leerFiltros({orden: 'raro'}).orden).toBe('rel');
  });
});
