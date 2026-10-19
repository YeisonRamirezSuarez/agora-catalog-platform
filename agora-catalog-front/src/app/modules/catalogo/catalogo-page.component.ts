import {afterRenderEffect, Component, computed, DestroyRef, ElementRef, inject, input, signal, viewChild} from '@angular/core';
import {ActivatedRoute, RouterLink} from '@angular/router';

import {CatalogoStore} from './catalogo.store';
import {cop, leerFiltros, lineas, plural} from './catalogo.util';
import {IconoComponent} from './icono.component';
import {TarjetaComponent} from './piezas.component';

/** Grupos de filtros: en computador van en la tarjeta de la izquierda; en celular, en la hoja "Filtros". */
@Component({
  selector: 'cat-filtros',
  host: {style: 'display:contents'},
  template: `
    @let f = store.filtros();
    @let lim = store.limites();
    <div class="f-group">
      <h3>Categoría</h3>
      @for (c of store.categorias(); track c.id) {
        <label class="f-check"><input type="checkbox" [checked]="f.cats.includes(c.id)" (change)="toggleCat(c.id)">{{ c.nombre }} <small>({{ cuenta(c.id) }})</small></label>
      }
    </div>
    @if (store.precios() && lim[1] > lim[0]) {
      <div class="f-group">
        <h3>Precio</h3>
        <p class="range-txt">{{ cop(f.min || lim[0]) }} - {{ cop(f.max || lim[1]) }}</p>
        <div class="range">
          <input type="range" [min]="lim[0]" [max]="lim[1]" step="500" [value]="f.min || lim[0]" aria-label="Precio mínimo" (input)="rango($any($event.target).value, null)">
          <input type="range" [min]="lim[0]" [max]="lim[1]" step="500" [value]="f.max || lim[1]" aria-label="Precio máximo" (input)="rango(null, $any($event.target).value)">
        </div>
      </div>
    }
    @if (store.precios() && store.enOferta().length) {
      <div class="f-group"><h3>Promociones</h3>
        <label class="f-check"><input type="checkbox" [checked]="f.promo" (change)="store.filtrar({promo: !f.promo})">Solo promociones <small>({{ store.enOferta().length }})</small></label>
      </div>
    }
    <div class="f-group"><h3>Disponibilidad</h3>
      <label class="f-check"><input type="checkbox" [checked]="f.disp" (change)="store.filtrar({disp: !f.disp})">En stock <small>({{ disponibles() }})</small></label>
    </div>
    @if (store.favoritos().size) {
      <div class="f-group"><h3>Favoritos</h3>
        <label class="f-check"><input type="checkbox" [checked]="f.favs" (change)="store.filtrar({favs: !f.favs})">Mis favoritos <small>({{ store.favoritos().size }})</small></label>
      </div>
    }
  `
})
export class FiltrosComponent {
  readonly store = inject(CatalogoStore);
  readonly cop = cop;
  readonly disponibles = computed(() => this.store.productos().filter(p => p.disponible).length);

  cuenta(cat: number): number {
    return this.store.productos().filter(p => p.categoriaId === cat).length;
  }

  toggleCat(id: number): void {
    const cats = this.store.filtros().cats;
    this.store.filtrar({cats: cats.includes(id) ? cats.filter(c => c !== id) : [...cats, id]});
  }

  rango(min: string | null, max: string | null): void {
    const [lo, hi] = this.store.limites(), f = this.store.filtros();
    let a = min !== null ? +min : (f.min || lo), b = max !== null ? +max : (f.max || hi);
    if (a > b) [a, b] = min !== null ? [b, b] : [a, a];
    this.store.filtrar({min: a > lo ? a : 0, max: b < hi ? b : 0});
  }
}

/** Página de catálogo: búsqueda, filtros, orden y vista; todo vive en la URL para compartir una búsqueda. */
@Component({
  selector: 'cat-catalogo',
  imports: [RouterLink, IconoComponent, TarjetaComponent, FiltrosComponent],
  host: {style: 'display:contents'},
  template: `
    @let n = store.negocio();
    @let f = store.filtros();
    <section class="cat-hero">
      @if (n.portadaUrl) { <img [src]="n.portadaUrl" alt=""> }
      <div class="cat-hero-in">
        @if (n.catalogoTitulo) { <p class="eyebrow">Nuestro catálogo</p> }
        <h1>
          @if (n.catalogoTitulo) {
            @for (l of lineas(n.catalogoTitulo); track $index) { @if (!$first) {<br>}{{ l }} }
            @if (n.catalogoAcento) { <em>@for (l of lineas(n.catalogoAcento); track $index) { @if (!$first) {<br>}{{ l }} }</em> }
          } @else { Nuestro catálogo }
        </h1>
        <p>{{ n.catalogoSubtitulo || plural(store.productos().length, 'producto') + ' de ' + n.nombre + '.' }}</p>
      </div>
    </section>
    <div class="cat-wrap">
      <nav class="crumbs" aria-label="Ruta">
        <a [routerLink]="store.base()"><svg catIcono="back"></svg>Inicio</a><span aria-hidden="true">/</span><span aria-current="page">Catálogo</span>
      </nav>
      <div class="cat-tools">
        <form class="c-search" role="search" (submit)="$event.preventDefault(); buscador.blur()">
          <svg catIcono="search"></svg>
          <label class="sr" for="cq">Buscar productos</label>
          <input #buscador id="cq" type="search" [value]="f.q" [placeholder]="store.pistaBusqueda()" autocomplete="off" enterkeyhint="search"
                 (input)="store.filtrar({q: $any($event.target).value.trim()})">
          <button aria-label="Buscar"><svg catIcono="search"></svg></button>
        </form>
        <button type="button" class="btn filters-btn" (click)="store.hoja.set({tipo: 'filtros'})">
          <svg catIcono="filter"></svg>Filtros<span>{{ activos() ? ' (' + activos() + ')' : '' }}</span>
        </button>
        <select class="c-select" aria-label="Ordenar" [value]="f.orden" (change)="store.filtrar({orden: $any($event.target).value})">
          <option value="rel">Más relevantes</option>
          <option value="menor">Menor precio</option>
          <option value="mayor">Mayor precio</option>
          <option value="az">Nombre de la A a la Z</option>
        </select>
        <div class="view-tog" role="group" aria-label="Vista">
          <button type="button" aria-label="Cuadrícula" [attr.aria-pressed]="!f.lista" (click)="store.filtrar({lista: false})"><svg catIcono="grid"></svg></button>
          <button type="button" aria-label="Lista" [attr.aria-pressed]="f.lista" (click)="store.filtrar({lista: true})"><svg catIcono="list"></svg></button>
        </div>
      </div>
      <div class="cat-layout">
        <aside class="filters" aria-label="Filtros">
          <div class="f-card"><cat-filtros /></div>
          @if (store.hayWhatsapp()) {
            <div class="help-card">
              <h3>¿No encuentras algo en específico?</h3>
              <p>Escríbenos por WhatsApp y te ayudamos.</p>
              <a class="btn" [href]="store.hrefWhatsapp(msgBusco)" target="_blank" rel="noopener" (click)="elegirNumero($event, msgBusco)"><svg catIcono="wa"></svg>Hablar por WhatsApp</a>
            </div>
          }
        </aside>
        <div>
          <div class="res-head">
            <p id="rcount" aria-live="polite">{{ plural(store.resultados().length, 'resultado') }}</p>
            <div class="active-f">
              @for (c of chips(); track c.k) {
                <button type="button" class="fchip" [attr.aria-label]="'Quitar el filtro ' + c.texto" (click)="quitar(c.k)">{{ c.texto }}<svg catIcono="x"></svg></button>
              }
            </div>
          </div>
          <div #grilla [class]="'grid' + (f.lista ? ' as-list' : '')">
            @for (p of store.resultados(); track p.id) { <cat-tarjeta [p]="p" /> }
            @if (store.resultados().length) {
              @if (mostrarPasos()) {
                <aside class="how" [class.wide]="libres() > 1" [style.grid-column]="'span ' + (libres() || 1)" aria-label="Cómo pedir">
                  <h3>Pide en tres pasos</h3>
                  <ol>
                    <li>Elige tus productos y cuántos quieres.</li>
                    <li>Revisa la lista en «Mi pedido».</li>
                    <li>Envíala por WhatsApp: {{ n.nombreCorto || n.nombre }} te confirma total y domicilio.</li>
                  </ol>
                  <button type="button" class="btn how-btn" (click)="store.hoja.set({tipo: 'pedido'})"><svg catIcono="cart"></svg>Ver mi pedido</button>
                </aside>
              }
            } @else {
              <div class="no-hits">
                <b>{{ f.q ? 'No encontramos “' + f.q + '”' : 'Ningún producto con estos filtros' }}</b>
                <span>Prueba con otra palabra o quita filtros{{ store.hayWhatsapp() ? ', o pregúntanos por WhatsApp' : '' }}.</span>
                <button type="button" class="btn" (click)="limpiar()">Quitar filtros</button>
              </div>
            }
          </div>
        </div>
      </div>
    </div>
  `
})
export class CatalogoPageComponent {
  readonly store = inject(CatalogoStore);
  private readonly route = inject(ActivatedRoute);
  /** El botón de buscar del encabezado pide enfocar el buscador. */
  readonly enfocar = input(0);
  readonly lineas = lineas;
  readonly plural = plural;
  readonly msgBusco = 'Hola, busco un producto que no encontré en el catálogo.';
  private readonly grilla = viewChild<ElementRef<HTMLElement>>('grilla');
  private readonly buscador = viewChild<ElementRef<HTMLInputElement>>('buscador');
  /** Celdas vacías en la última fila de la cuadrícula. */
  readonly libres = signal(0);

  constructor() {
    const sub = this.route.queryParams.subscribe(q => this.store.filtros.set(leerFiltros(q)));
    inject(DestroyRef).onDestroy(() => sub.unsubscribe());

    // La tarjeta "Pide en tres pasos" llena los huecos de la última fila; se recalcula al cambiar el ancho.
    const medir = () => {
      const el = this.grilla()?.nativeElement;
      if (!el) return;
      const cols = getComputedStyle(el).gridTemplateColumns.split(' ').length;
      this.libres.set((cols - this.store.resultados().length % cols) % cols);
    };
    const ro = new ResizeObserver(medir);
    afterRenderEffect(() => {
      this.store.resultados();
      const el = this.grilla()?.nativeElement;
      if (el) {
        ro.observe(el);
        medir();
      }
    });
    afterRenderEffect(() => {
      if (this.enfocar()) this.buscador()?.nativeElement.focus({preventScroll: true});
    });
    inject(DestroyRef).onDestroy(() => ro.disconnect());
  }

  readonly mostrarPasos = computed(() =>
    this.store.pedidos() && this.store.hayWhatsapp() && !this.store.filtros().lista && this.libres() > 0);

  readonly activos = computed(() => {
    const f = this.store.filtros();
    return (f.col ? 1 : 0) + f.cats.length + +f.promo + +f.mas + +f.disp + +f.favs + (f.min || f.max ? 1 : 0);
  });

  readonly chips = computed(() => {
    const f = this.store.filtros(), [lo, hi] = this.store.limites(), L: {k: string; texto: string}[] = [];
    const col = this.store.data()?.colecciones.find(c => c.id === f.col);
    if (col) L.push({k: 'col', texto: col.nombre});
    for (const id of f.cats) {
      const c = this.store.categorias().find(x => x.id === id);
      if (c) L.push({k: `cat:${id}`, texto: c.nombre});
    }
    if (f.promo) L.push({k: 'promo', texto: 'Promociones'});
    if (f.mas) L.push({k: 'mas', texto: 'Más pedidos'});
    if (f.disp) L.push({k: 'disp', texto: 'En stock'});
    if (f.favs) L.push({k: 'favs', texto: 'Mis favoritos'});
    if (f.min || f.max) L.push({k: 'precio', texto: `${cop(f.min || lo)} - ${cop(f.max || hi)}`});
    return L;
  });

  quitar(k: string): void {
    const f = this.store.filtros();
    if (k.startsWith('cat:')) this.store.filtrar({cats: f.cats.filter(c => c !== +k.slice(4))});
    else if (k === 'precio') this.store.filtrar({min: 0, max: 0});
    else if (k === 'col') this.store.filtrar({col: 0});
    else this.store.filtrar({[k]: false});
  }

  limpiar(): void {
    this.store.filtrar({cats: [], promo: false, mas: false, disp: false, favs: false, col: 0, q: '', min: 0, max: 0});
    this.buscador()?.nativeElement.focus();
  }

  elegirNumero(e: Event, mensaje: string): void {
    if (this.store.whatsapps().length > 1) {
      e.preventDefault();
      this.store.escribir(mensaje);
    }
  }
}
