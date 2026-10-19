import {Component, computed, effect, ElementRef, inject, input, output, viewChild} from '@angular/core';
import {RouterLink} from '@angular/router';

import {CatalogoStore} from './catalogo.store';
import {cop, plural} from './catalogo.util';
import {IconoComponent} from './icono.component';

/** Una entrada del menú del catálogo. */
export interface ItemMenu {
  clave: 'inicio' | 'catalogo' | 'promos' | 'combos' | 'contacto';
  texto: string;
  ruta: string;
  query: Record<string, string>;
}

/** Inicio, Catálogo, Promociones, la categoría con franja y Contáctanos (que baja a la sección de contacto). */
export function menuCatalogo(store: CatalogoStore): ItemMenu[] {
  const base = store.base(), franja = store.franja();
  return [
    {clave: 'inicio', texto: 'Inicio', ruta: base, query: {}},
    {clave: 'catalogo', texto: 'Catálogo', ruta: `${base}/catalogo`, query: {}},
    ...(store.precios() && store.enOferta().length
      ? [{clave: 'promos' as const, texto: 'Promociones', ruta: `${base}/catalogo`, query: {promo: '1'}}] : []),
    ...(franja ? [{clave: 'combos' as const, texto: franja.categoria.nombre, ruta: `${base}/catalogo`,
      query: {cat: String(franja.categoriaId)}}] : []),
    {clave: 'contacto', texto: 'Contáctanos', ruta: `${base}/contacto`, query: {}},
  ];
}

/** Iniciales para el logo cuando el negocio no subió uno. */
const iniciales = (s: string) => s.split(/\s+/).filter(w => w.length > 2 || /^[A-ZÁÉÍÓÚ]/.test(w))
  .slice(0, 2).map(w => w[0]).join('').toUpperCase();

/** Logo y nombre del negocio: «Salsamentaria» pequeño encima de «Javier» grande, y el lema debajo. */
@Component({
  selector: 'cat-marca',
  imports: [RouterLink],
  host: {style: 'display:contents'},
  template: `
    <a class="brandlink" [routerLink]="store.base()" [attr.aria-label]="n().nombre + ': inicio'">
      @if (n().logoUrl) {
        <span class="logo has-img" aria-hidden="true"><img [src]="n().logoUrl" alt=""></span>
      } @else {
        <span class="logo" aria-hidden="true">{{ ini() }}</span>
      }
      <span class="bn">
        @if (corto()) { <small>{{ antes() }}</small> }
        <b>{{ corto() || n().nombre }}</b>
        @if (n().lema) { <i>{{ n().lema }}</i> }
      </span>
    </a>
  `
})
export class MarcaComponent {
  readonly store = inject(CatalogoStore);
  readonly n = this.store.negocio;
  readonly ini = computed(() => iniciales(this.n().nombre));
  readonly corto = computed(() => {
    const {nombre, nombreCorto: c} = this.n();
    return c && nombre !== c && nombre.endsWith(c) ? c : '';
  });
  readonly antes = computed(() => this.n().nombre.slice(0, -this.corto().length).trim());
}

/** Encabezado fijo sobre la portada; al bajar se vuelve sólido. */
@Component({
  selector: 'cat-cabecera',
  imports: [RouterLink, IconoComponent, MarcaComponent],
  host: {style: 'display:contents'},
  template: `
    <header class="top" id="top" [class.solid]="solido()">
      <div class="top-in">
        <button type="button" class="icon-btn menu-btn" aria-label="Menú" (click)="store.hoja.set({tipo: 'menu'})">
          <svg catIcono="menu"></svg>
        </button>
        <cat-marca />
        <nav class="nav" aria-label="Secciones">
          @for (m of menu(); track m.clave) {
            <a class="nav-a" [routerLink]="m.ruta" [queryParams]="m.query" [attr.aria-current]="m.clave === activo() ? 'page' : null">{{ m.texto }}</a>
          }
        </nav>
        <button type="button" class="icon-btn find-btn" aria-label="Buscar productos" (click)="buscar.emit()">
          <svg catIcono="search"></svg>
        </button>
        @if (store.pedidos()) {
          <button type="button" class="cart-btn" (click)="store.hoja.set({tipo: 'pedido'})">
            <span class="cart-ic" #icono>
              <svg catIcono="cart"></svg>
              <span class="cart-n" [hidden]="!store.unidades()">{{ store.unidades() }}</span>
            </span>
            <span class="sr">{{ textoSr() }}</span>
            <span class="cart-txt" aria-hidden="true"><small>Mi pedido</small><b>{{ textoTotal() }}</b></span>
          </button>
        }
      </div>
    </header>
  `
})
export class CabeceraComponent {
  readonly store = inject(CatalogoStore);
  readonly solido = input(false);
  readonly activo = input<ItemMenu['clave']>('inicio');
  readonly buscar = output<void>();
  private readonly icono = viewChild<ElementRef<HTMLElement>>('icono');

  constructor() {
    // Al agregar, el carrito da un saltito: se quita la clase, se fuerza el reflujo y se vuelve a poner.
    effect(() => {
      const veces = this.store.animarCarrito(), el = this.icono()?.nativeElement;
      if (!veces || !el) return;
      el.classList.remove('bump');
      void el.offsetWidth;
      el.classList.add('bump');
    });
  }
  readonly menu = computed(() => menuCatalogo(this.store));
  readonly textoTotal = computed(() => this.store.precios() ? cop(this.store.total()) : plural(this.store.unidades(), 'producto'));
  readonly textoSr = computed(() => {
    const n = this.store.unidades();
    return n ? `Ver mi pedido: ${plural(n, 'producto')}${this.store.precios() ? `, ${cop(this.store.total())}` : ''}` : 'Ver mi pedido';
  });
}

/** Pie oscuro: marca del negocio, menú, compartir y "Catálogo hecho con Agorá Catalog". */
@Component({
  selector: 'cat-pie',
  imports: [RouterLink, IconoComponent, MarcaComponent],
  host: {style: 'display:contents'},
  template: `
    <footer class="foot">
      <div class="foot-in">
        <div class="foot-brand">
          <cat-marca />
          @if (n().portadaSubtitulo) { <p>{{ n().portadaSubtitulo }}</p> }
        </div>
        <nav class="foot-nav" aria-labelledby="ft-nav">
          <h3 id="ft-nav">Explora</h3>
          @for (m of menu(); track m.clave) {
            <a [routerLink]="m.ruta" [queryParams]="m.query">{{ m.texto }}</a>
          }
        </nav>
        <div class="foot-share">
          <h3>Comparte el catálogo</h3>
          <p>Envíalo a tu familia o a tus amigos por WhatsApp o redes.</p>
          <button type="button" class="btn foot-btn" (click)="store.compartir()"><svg catIcono="share"></svg>Compartir catálogo</button>
        </div>
      </div>
      <div class="foot-legal">
        <span>© {{ anio }} {{ n().nombre }}.@if (!store.precios()) { Pregunta precios y disponibilidad por WhatsApp.}</span>
        <span class="foot-made">Catálogo hecho con <img class="ec" src="assets/images/brand/logo-dark.svg" alt="Agorá Catalog" width="132" height="28"></span>
      </div>
    </footer>
  `
})
export class PieComponent {
  readonly store = inject(CatalogoStore);
  readonly n = this.store.negocio;
  readonly menu = computed(() => menuCatalogo(this.store));
  readonly anio = new Date().getFullYear();
}
