import {Component, computed, inject, input, signal} from '@angular/core';

import {Producto} from './catalogo.models';
import {CatalogoStore} from './catalogo.store';
import {cop, msgProducto, UNIDADES} from './catalogo.util';
import {IconoComponent} from './icono.component';

/** Foto del producto, o el ícono de foto si todavía no tiene. */
@Component({
  selector: 'cat-foto',
  imports: [IconoComponent],
  host: {'[class]': '"ph " + clase()'},
  template: `
    @if (src()) {
      <img [src]="src()" alt="" loading="lazy" decoding="async">
    } @else {
      <span class="ph-none"><svg catIcono="photo"></svg></span>
    }
  `
})
export class FotoComponent {
  readonly src = input<string | null>(null);
  readonly clase = input('');
}

/** Precio con unidad y precio anterior tachado; sin precios visibles, "Pregunta el precio". */
@Component({
  selector: 'cat-precio',
  template: `
    @if (!store.precios()) {
      <span [class]="clase() + ' ask'">Pregunta el precio</span>
    } @else {
      <span [class]="clase()">{{ cop(p().precioFinal ?? 0) }}@if (unidad()) {<small> / {{ unidad() }}</small>}@if (p().ofertaVigente) {<s><span class="sr">Antes </span>{{ cop(p().precio ?? 0) }}</s>}</span>
    }
  `,
  host: {style: 'display:contents'}
})
export class PrecioComponent {
  readonly store = inject(CatalogoStore);
  readonly p = input.required<Producto>();
  readonly clase = input('price');
  readonly cop = cop;
  // La unidad es la de la presentación principal: "Media libra" no se vende "/ libra".
  readonly unidad = computed(() => this.p().variante || this.p().unidadVenta === 'UNIDAD' ? '' : UNIDADES[this.p().unidadVenta]);
}

/** Una sola etiqueta, en este orden: Agotado > Promo > Más vendido > Nuevo. */
@Component({
  selector: 'cat-etiqueta',
  template: `
    @if (!p().disponible) {
      <span class="tagb out">Agotado</span>
    } @else if (store.precios() && p().ofertaVigente) {
      <span class="tagb promo">Promo<span class="sr">, {{ p().descuento }}% menos</span></span>
    } @else if (p().destacado) {
      <span class="tagb best">Más vendido</span>
    } @else if (p().nuevo) {
      <span class="tagb new">Nuevo</span>
    }
  `,
  host: {style: 'display:contents'}
})
export class EtiquetaComponent {
  readonly store = inject(CatalogoStore);
  readonly p = input.required<Producto>();
}

/** Selector de cantidad (− 1 +) antes de agregar al pedido. */
@Component({
  selector: 'cat-cantidad',
  imports: [IconoComponent],
  template: `
    <div [class]="'qty ' + clase()" role="group" aria-label="Cantidad">
      <button type="button" aria-label="Uno menos" [attr.aria-disabled]="n() <= 1" (click)="cambiar(-1)"><svg catIcono="minus"></svg></button>
      <output>{{ n() }}</output>
      <button type="button" aria-label="Uno más" [attr.aria-disabled]="n() >= 99" (click)="cambiar(1)"><svg catIcono="plus"></svg></button>
    </div>
  `,
  host: {style: 'display:contents'}
})
export class CantidadComponent {
  readonly clase = input('');
  readonly n = signal(1);

  cambiar(d: number): void {
    this.n.update(v => Math.min(99, Math.max(1, v + d)));
  }
}

/** Tarjeta de producto del inicio y de la página de catálogo. */
@Component({
  selector: 'cat-tarjeta',
  imports: [IconoComponent, FotoComponent, PrecioComponent, EtiquetaComponent, CantidadComponent],
  host: {'[class]': '"pc" + (p().disponible ? "" : " out")', role: 'article'},
  template: `
    @if (tieneEtiqueta()) {
      <span class="pc-badge"><cat-etiqueta [p]="p()" /></span>
    }
    <button type="button" class="heart" [attr.aria-pressed]="store.esFavorito(p().id)"
            [attr.aria-label]="'Guardar ' + p().nombre + ' en favoritos'" (click)="store.toggleFavorito(p().id)">
      <svg catIcono="heart"></svg>
    </button>
    <button type="button" class="pc-hit" (click)="store.hoja.set({tipo: 'producto', id: p().id})">
      <cat-foto [src]="p().imagenThumbUrl ?? p().imagenUrl" />
      <span class="pc-body">
        <span class="pc-name">{{ p().nombre }}</span>
        @if (p().descripcion) { <span class="pc-desc">{{ p().descripcion }}</span> } @else { <span></span> }
        <span class="pc-foot">
          <cat-precio [p]="p()" />
          @if (p().presentacion) { <span class="pres">{{ p().presentacion }}@if (p().presentaciones.length) { y {{ p().presentaciones.length }} más}</span> }
        </span>
      </span>
    </button>
    @if (!store.pedidos()) {
      @if (store.hayWhatsapp() && p().disponible) {
        <div class="buy">
          <a class="btn ask-wa" [href]="store.hrefWhatsapp(msg())" target="_blank" rel="noopener" (click)="preguntar($event)">
            <svg catIcono="wa"></svg>Preguntar
          </a>
        </div>
      }
    } @else if (p().disponible) {
      <div class="buy">
        <cat-cantidad #cant />
        <button type="button" class="add" [attr.aria-label]="(p().presentaciones.length ? 'Elegir la presentación de ' : 'Agregar ') + p().nombre + (p().presentaciones.length ? '' : ' al pedido')"
                (click)="agregar(cant.n()); cant.n.set(1)">
          <svg catIcono="cart"></svg>
        </button>
      </div>
    } @else {
      <div class="buy"><span class="soon">Agotado por ahora</span></div>
    }
  `
})
export class TarjetaComponent {
  readonly store = inject(CatalogoStore);
  readonly p = input.required<Producto>();
  readonly msg = computed(() => msgProducto(this.p()));
  readonly tieneEtiqueta = computed(() => {
    const p = this.p();
    return !p.disponible || (this.store.precios() && !!p.ofertaVigente) || p.destacado || p.nuevo;
  });

  /** Con otras presentaciones se abre su hoja para elegir cuál; si no, se agrega directo. */
  agregar(n: number): void {
    if (this.p().presentaciones.length) this.store.hoja.set({tipo: 'producto', id: this.p().id});
    else this.store.agregar(this.p(), n);
  }

  /** Con varios números, primero se elige a cuál escribir. */
  preguntar(e: Event): void {
    if (this.store.whatsapps().length > 1) {
      e.preventDefault();
      this.store.escribir(this.msg());
    }
  }
}
