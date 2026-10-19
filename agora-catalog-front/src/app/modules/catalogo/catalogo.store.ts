import {computed, inject, Injectable, signal} from '@angular/core';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {Router} from '@angular/router';

import {CatalogoPublico, Filtros, Hoja, Producto} from './catalogo.models';
import {efectivo, FILTROS_VACIOS, filtrosAQuery, LineaPedido, MSG_GENERAL, norm, porRelevancia, waHref} from './catalogo.util';

type Estado = 'cargando' | 'listo' | 'no-disponible' | 'error';
/** Lo que el catálogo cuenta para las estadísticas del negocio (sin datos de quien visita). */
export type Evento = 'VISITA' | 'PRODUCTO' | 'PEDIDO' | 'WHATSAPP';

/**
 * Estado del catálogo público de un negocio. Se crea con la página (providers del shell) y muere con
 * ella. El pedido y los favoritos viven solo en el navegador del visitante (localStorage por negocio):
 * no hay cuentas ni pedidos en el servidor.
 */
@Injectable()
export class CatalogoStore {

  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  readonly estado = signal<Estado>('cargando');
  readonly data = signal<CatalogoPublico | null>(null);
  readonly pedido = signal<Record<number, number>>({});
  readonly favoritos = signal<ReadonlySet<number>>(new Set());
  readonly hoja = signal<Hoja | null>(null);
  readonly aviso = signal('');
  readonly filtros = signal<Filtros>(FILTROS_VACIOS);
  readonly animarCarrito = signal(0);
  private avisoTimer?: ReturnType<typeof setTimeout>;

  readonly negocio = computed(() => this.data()!.negocio);
  readonly precios = computed(() => this.negocio().mostrarPrecios);
  readonly pedidos = computed(() => this.negocio().pedidosWhatsapp);
  readonly whatsapps = computed(() => this.data()?.whatsapps ?? []);
  readonly hayWhatsapp = computed(() => this.whatsapps().length > 0);
  readonly base = computed(() => `/c/${this.negocio().slug}-${this.negocio().publicHash}`);
  readonly productos = computed(() => this.data()?.productos ?? []);
  readonly porId = computed(() => new Map(this.productos().map(p => [p.id, p])));
  readonly categorias = computed(() => (this.data()?.categorias ?? [])
    .filter(c => this.productos().some(p => p.categoriaId === c.id)));
  readonly franja = computed(() => {
    const f = this.data()?.franja;
    const cat = f && this.categorias().find(c => c.id === f.categoriaId);
    return f && cat ? {...f, categoria: cat} : null;
  });
  readonly enOferta = computed(() => this.productos().filter(p => p.ofertaVigente && p.disponible));

  readonly lineas = computed<LineaPedido[]>(() => Object.entries(this.pedido())
    .map(([id, cantidad]) => ({producto: this.porId().get(+id)!, cantidad}))
    .filter(l => l.producto));
  readonly unidades = computed(() => this.lineas().reduce((s, l) => s + l.cantidad, 0));
  readonly total = computed(() => this.lineas().reduce((s, l) => s + efectivo(l.producto) * l.cantidad, 0));

  /** El link se resuelve por el hash (lo que va después del último guion); el slug es solo legible. */
  cargar(publicId: string): void {
    const hash = publicId.split('-').pop() ?? '';
    this.estado.set('cargando');
    this.http.get<CatalogoPublico>(`/public/catalogos/${encodeURIComponent(hash)}`).subscribe({
      next: data => {
        this.data.set(data);
        this.leerNavegador();
        this.estado.set('listo');
        this.registrar('VISITA');
        // Slug viejo o mal escrito: se corrige la URL sin romper el link (en producción también responde el backend).
        const correcto = `${data.negocio.slug}-${data.negocio.publicHash}`;
        if (publicId !== correcto) {
          const url = this.router.url.replace(`/c/${publicId}`, `/c/${correcto}`);
          void this.router.navigateByUrl(url, {replaceUrl: true});
        }
      },
      error: (e: unknown) => this.estado.set(e instanceof HttpErrorResponse && e.status === 404 ? 'no-disponible' : 'error')
    });
  }

  // ---------- Pedido ----------

  cantidad(id: number): number {
    return this.pedido()[id] ?? 0;
  }

  setCantidad(id: number, n: number): void {
    const next = {...this.pedido()};
    if (n > 0) next[id] = Math.min(n, 99); else delete next[id];
    this.pedido.set(next);
    this.guardar('pedido', next);
  }

  agregar(p: Producto, n: number): void {
    this.setCantidad(p.id, this.cantidad(p.id) + n);
    this.animarCarrito.update(v => v + 1);
    this.avisar(`Agregado a tu pedido: ${n} × ${p.nombre}`);
  }

  vaciar(): void {
    this.pedido.set({});
    this.guardar('pedido', {});
    this.avisar('Tu pedido quedó vacío');
  }

  // ---------- Favoritos ----------

  esFavorito(id: number): boolean {
    return this.favoritos().has(id);
  }

  toggleFavorito(id: number): void {
    const next = new Set(this.favoritos());
    const on = !next.has(id);
    if (on) next.add(id); else next.delete(id);
    this.favoritos.set(next);
    this.guardar('favoritos', [...next]);
    if (!next.size && this.filtros().favs) this.filtros.update(f => ({...f, favs: false}));
    this.avisar(on ? 'Guardado en tus favoritos' : 'Quitado de tus favoritos');
  }

  // ---------- WhatsApp ----------

  /** Con un número abre WhatsApp directo; con varios, la hoja para elegir a cuál escribir. */
  escribir(mensaje = MSG_GENERAL): void {
    const numeros = this.whatsapps();
    if (numeros.length === 1) {
      this.registrar('WHATSAPP');
      window.open(waHref(numeros[0].numero, mensaje), '_blank', 'noopener');
    } else if (numeros.length > 1) {
      this.hoja.set({tipo: 'whatsapp', mensaje});
    }
  }

  hrefWhatsapp(mensaje = MSG_GENERAL): string {
    const n = this.whatsapps()[0]?.numero;
    return n ? waHref(n, mensaje) : '#';
  }

  // ---------- Filtros de la página de catálogo ----------

  readonly limites = computed<[number, number]>(() => {
    const ps = this.productos().map(efectivo);
    return ps.length ? [Math.floor(Math.min(...ps) / 1000) * 1000, Math.ceil(Math.max(...ps) / 1000) * 1000] : [0, 0];
  });

  readonly resultados = computed(() => {
    const f = this.filtros(), q = norm(f.q), precios = this.precios();
    const col = this.data()?.colecciones.find(c => c.id === f.col);
    const cats = new Map(this.categorias().map(c => [c.id, c.nombre]));
    const L = this.productos().filter(p =>
      (!f.cats.length || f.cats.includes(p.categoriaId))
      && (!f.promo || (precios && !!p.ofertaVigente)) && (!f.mas || p.destacado) && (!f.disp || p.disponible)
      && (!f.favs || this.favoritos().has(p.id)) && (!col || col.productos.includes(p.id))
      && (!q || norm(`${p.nombre} ${p.presentacion ?? ''} ${p.descripcion ?? ''} ${cats.get(p.categoriaId) ?? ''}`).includes(q))
      && (!precios || ((!f.min || efectivo(p) >= f.min) && (!f.max || efectivo(p) <= f.max))));
    if (f.orden === 'menor') return L.sort((a, b) => efectivo(a) - efectivo(b));
    if (f.orden === 'mayor') return L.sort((a, b) => efectivo(b) - efectivo(a));
    if (f.orden === 'az') return L.sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));
    return porRelevancia(L);
  });

  /** Cambia filtros y los deja en la URL (replaceUrl: escribir en el buscador no llena el historial). */
  filtrar(cambio: Partial<Filtros>): void {
    this.filtros.update(f => ({...f, ...cambio}));
    void this.router.navigate([this.base(), 'catalogo'], {queryParams: filtrosAQuery(this.filtros()), replaceUrl: true});
  }

  /** "Busca salchichón, queso, …" con los primeros productos. */
  readonly pistaBusqueda = computed(() => `Busca ${[...new Set(porRelevancia(this.productos())
    .map(p => p.nombre.split(' ')[0].toLowerCase()))].slice(0, 4).join(', ')}…`);

  // ---------- Avisos y compartir ----------

  avisar(mensaje: string): void {
    this.aviso.set(mensaje);
    clearTimeout(this.avisoTimer);
    this.avisoTimer = setTimeout(() => this.aviso.set(''), 2800);
  }

  /** El catálogo, o un producto: su link abre la hoja del producto y en WhatsApp sale con su foto y precio. */
  async compartir(p?: Producto): Promise<void> {
    const url = location.origin + this.base() + (p ? `/catalogo?p=${p.id}` : '');
    const title = p ? `${p.nombre} · ${this.negocio().nombre}` : this.negocio().nombre;
    try {
      if (navigator.share) {
        await navigator.share({title, url});
      } else {
        await navigator.clipboard.writeText(url);
        this.avisar('Link copiado');
      }
    } catch {
      // El visitante cerró el diálogo de compartir.
    }
  }

  // ---------- Navegador ----------

  private clave(k: string): string {
    return `ec-${k}-${this.negocio().publicHash}`;
  }

  // ---------- Estadísticas ----------

  /**
   * Avisa al backend para las estadísticas del dueño. Visita y producto cuentan una vez por sesión
   * del navegador (recargar no infla); pedido y WhatsApp, cada vez. Con una sesión del panel abierta
   * en este navegador no cuenta: es el dueño o alguien probando, no un cliente.
   */
  registrar(tipo: Evento, productoId?: number): void {
    const hash = this.data()?.negocio.publicHash;
    if (!hash) return;
    try {
      if (localStorage.getItem('ac-sesion')) return;
      if (tipo === 'VISITA' || tipo === 'PRODUCTO') {
        const clave = `ac-ev-${hash}-${tipo}-${productoId ?? ''}`;
        if (sessionStorage.getItem(clave)) return;
        sessionStorage.setItem(clave, '1');
      }
    } catch {
      // Sin almacenamiento se cuenta igual.
    }
    // keepalive: el aviso sale aunque el clic abra WhatsApp y el navegador deje la página.
    void fetch(`/api/public/catalogos/${encodeURIComponent(hash)}/eventos`, {
      method: 'POST', keepalive: true, headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({tipo, productoId: productoId ?? null})
    }).catch(() => undefined);
  }

  private guardar(k: string, v: unknown): void {
    try {
      localStorage.setItem(this.clave(k), JSON.stringify(v));
    } catch {
      // Sin almacenamiento (modo privado) dura mientras la página esté abierta.
    }
  }

  private leerNavegador(): void {
    const leer = <T>(k: string, vacio: T): T => {
      try {
        return JSON.parse(localStorage.getItem(this.clave(k)) ?? 'null') ?? vacio;
      } catch {
        return vacio;
      }
    };
    // Los productos que ya no están disponibles salen del pedido al cargar.
    const pedido: Record<number, number> = {};
    for (const [id, n] of Object.entries(leer<Record<string, number>>('pedido', {}))) {
      if (this.porId().get(+id)?.disponible && n > 0) pedido[+id] = Math.min(n, 99);
    }
    this.pedido.set(pedido);
    this.favoritos.set(new Set(leer<number[]>('favoritos', []).filter(id => this.porId().has(id))));
  }
}
