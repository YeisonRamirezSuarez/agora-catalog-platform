import {TestBed} from '@angular/core/testing';
import {vi} from 'vitest';

import {commonTestProviders} from 'src/testing/test-providers';
import {CatalogoPublico, Producto} from './catalogo.models';
import {CatalogoStore} from './catalogo.store';
import {conPresentacion} from './catalogo.util';

describe('CatalogoStore.registrar', () => {
  let store: CatalogoStore;
  let fetchSpy: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    fetchSpy = vi.fn().mockResolvedValue(new Response(null, {status: 204}));
    vi.stubGlobal('fetch', fetchSpy);
    TestBed.configureTestingModule({providers: [...commonTestProviders, CatalogoStore]});
    store = TestBed.inject(CatalogoStore);
    store.data.set({negocio: {publicHash: 'x7k2m9ab'}} as unknown as CatalogoPublico);
  });

  afterEach(() => vi.unstubAllGlobals());

  const cuerpos = () => fetchSpy.mock.calls.map(c => JSON.parse((c[1] as RequestInit).body as string));

  it('cuenta la visita y cada producto una sola vez por sesión', () => {
    store.registrar('VISITA');
    store.registrar('VISITA');
    store.registrar('PRODUCTO', 5);
    store.registrar('PRODUCTO', 5);
    store.registrar('PRODUCTO', 6);
    expect(cuerpos()).toEqual([{tipo: 'VISITA', productoId: null}, {tipo: 'PRODUCTO', productoId: 5}, {tipo: 'PRODUCTO', productoId: 6}]);
    expect(fetchSpy.mock.calls[0][0]).toBe('/api/public/catalogos/x7k2m9ab/eventos');
  });

  it('cuenta cada pedido y cada clic a WhatsApp', () => {
    store.registrar('PEDIDO');
    store.registrar('PEDIDO');
    store.registrar('WHATSAPP');
    expect(cuerpos().map(c => c.tipo)).toEqual(['PEDIDO', 'PEDIDO', 'WHATSAPP']);
  });

  it('no cuenta nada con una sesión del panel abierta en el navegador', () => {
    localStorage.setItem('ac-sesion', '{}');
    store.registrar('VISITA');
    store.registrar('PEDIDO');
    expect(fetchSpy).not.toHaveBeenCalled();
  });
});

describe('CatalogoStore: pedido con otras presentaciones', () => {
  const queso = {
    id: 1, nombre: 'Queso', presentacion: 'Libra', unidadVenta: 'LIBRA', precio: 18000, precioFinal: 18000, disponible: true,
    presentaciones: [{nombre: 'Media libra', precio: 9500, precioFinal: 9000, ofertaVigente: true}]
  } as Producto;
  let store: CatalogoStore;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({providers: [...commonTestProviders, CatalogoStore]});
    store = TestBed.inject(CatalogoStore);
    store.data.set({negocio: {publicHash: 'p1'}, productos: [queso]} as unknown as CatalogoPublico);
  });

  it('cada presentación es su propia línea, con su precio, y vuelve del navegador', () => {
    store.agregar(queso, 1);
    store.agregar(conPresentacion(queso, queso.presentaciones[0]), 2);
    const lineas = () => store.lineas().map(l => [l.clave, l.producto.presentacion, l.cantidad]);
    expect(lineas()).toEqual([['1', 'Libra', 1], ['1|Media libra', 'Media libra', 2]]);
    expect(store.total()).toBe(18000 + 2 * 9000);

    // Una presentación que el negocio quitó sale del pedido al volver.
    localStorage.setItem('ec-pedido-p1', JSON.stringify({'1': 1, '1|Media libra': 2, '1|Kilo': 3}));
    store['leerNavegador']();
    expect(lineas()).toEqual([['1', 'Libra', 1], ['1|Media libra', 'Media libra', 2]]);
  });
});
