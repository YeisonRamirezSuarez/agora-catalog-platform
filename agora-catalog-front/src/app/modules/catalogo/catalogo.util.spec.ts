import {Producto} from './catalogo.models';
import {cop, filtrosAQuery, leerFiltros, mensajePedido, norm, phone} from './catalogo.util';

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
    expect(mensajePedido([{producto: queso, cantidad: 2}], true, 'Ana'))
      .toBe('Hola, quiero hacer este pedido:\n• 2 × Queso campesino (Bloque): $24.000\nTotal: $24.000\nNombre: Ana');
    expect(mensajePedido([{producto: queso, cantidad: 2}], false))
      .toBe('Hola, quiero hacer este pedido:\n• 2 × Queso campesino (Bloque)');
  });

  it('los filtros van y vuelven de la URL', () => {
    const f = leerFiltros({cat: '1,3', promo: '1', q: 'queso', orden: 'menor'});
    expect(f.cats).toEqual([1, 3]);
    expect(filtrosAQuery(f)).toEqual({cat: '1,3', promo: '1', q: 'queso', orden: 'menor'});
    expect(leerFiltros({orden: 'raro'}).orden).toBe('rel');
  });
});
