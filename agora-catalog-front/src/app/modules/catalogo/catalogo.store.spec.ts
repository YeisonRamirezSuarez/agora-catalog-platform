import {TestBed} from '@angular/core/testing';
import {vi} from 'vitest';

import {commonTestProviders} from 'src/testing/test-providers';
import {CatalogoPublico} from './catalogo.models';
import {CatalogoStore} from './catalogo.store';

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
