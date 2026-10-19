import {afterRenderEffect, Component, computed, effect, ElementRef, inject, viewChild} from '@angular/core';
import {RouterLink} from '@angular/router';

import {CatalogoStore} from './catalogo.store';
import {cop, efectivo, mensajePedido, msgProducto, phone, plural, waHref} from './catalogo.util';
import {FiltrosComponent} from './catalogo-page.component';
import {IconoComponent} from './icono.component';
import {menuCatalogo} from './marco.component';
import {CantidadComponent, EtiquetaComponent, FotoComponent, PrecioComponent} from './piezas.component';

/**
 * Hojas del catálogo: producto abierto, Mi pedido, menú, filtros y elegir número de WhatsApp. En celular
 * suben desde abajo; en computador son un modal (o un panel lateral, Mi pedido).
 */
@Component({
  selector: 'cat-hojas',
  imports: [RouterLink, IconoComponent, FotoComponent, PrecioComponent, EtiquetaComponent, CantidadComponent, FiltrosComponent],
  host: {style: 'display:contents', '(document:keydown.escape)': 'cerrar()'},
  template: `
    @if (store.hoja(); as h) {
      <div class="sheet-bg" (click)="$event.target === $event.currentTarget ? cerrar() : null">
        <div [class]="'sheet cat-sheet' + (h.tipo === 'pedido' ? ' drawer' : '')" role="dialog" aria-modal="true" [attr.aria-label]="titulo()">
          <button #cerrarBtn type="button" class="round sheet-close" aria-label="Cerrar" (click)="cerrar()"><svg catIcono="x"></svg></button>

          @switch (h.tipo) {
            @case ('producto') {
              @if (producto(); as p) {
                <span class="grab" aria-hidden="true"></span>
                <div class="sheet-scroll">
                  <span class="pc-badge"><cat-etiqueta [p]="p" /></span>
                  <cat-foto clase="sheet-ph" [src]="p.imagenUrl ?? p.imagenThumbUrl" />
                  <div class="ps-body">
                    <h2>{{ p.nombre }}</h2>
                    @if (p.presentacion) { <p class="muted">{{ p.presentacion }}</p> }
                    @if (p.disponible) { <cat-precio [p]="p" clase="price big" /> }
                    @if (p.descripcion) { <p class="ps-desc">{{ p.descripcion }}</p> }
                    <button type="button" class="ps-share" (click)="store.compartir(p)"><svg catIcono="share"></svg>Compartir este producto</button>
                  </div>
                </div>
                @if ((store.pedidos() && p.disponible) || store.hayWhatsapp()) {
                  <div class="ps-foot">
                    @if (store.pedidos() && p.disponible) {
                      <div class="ps-buy">
                        <cat-cantidad clase="big" #cant />
                        <button type="button" class="btn btn-brand" (click)="store.agregar(p, cant.n()); cerrar()"><svg catIcono="cart"></svg>Agregar al pedido</button>
                      </div>
                    }
                    @if (store.hayWhatsapp()) {
                      <a [class]="store.pedidos() && p.disponible ? 'ps-ask' : 'btn btn-brand ps-ask-btn'" [href]="store.hrefWhatsapp(msgProducto(p))"
                         target="_blank" rel="noopener" (click)="elegirNumero($event, msgProducto(p))">
                        <svg catIcono="wa"></svg>{{ p.disponible ? 'Preguntar por WhatsApp' : 'Preguntar cuándo llega' }}
                      </a>
                    }
                  </div>
                }
              }
            }

            @case ('pedido') {
              <div class="sheet-scroll">
                <div class="order" id="order">
                  <h2>Mi pedido</h2>
                  @if (!store.lineas().length) {
                    <div class="o-empty">
                      <p>Tu pedido está vacío. Suma productos con el botón del carrito.</p>
                      <a class="btn" [routerLink]="[store.base(), 'catalogo']" (click)="cerrar()">Explorar el catálogo</a>
                    </div>
                  } @else {
                    <ul class="olines">
                      @for (l of store.lineas(); track l.producto.id) {
                        <li class="oline">
                          <cat-foto [src]="l.producto.imagenThumbUrl ?? l.producto.imagenUrl" />
                          <div class="oline-txt">
                            <b>{{ l.producto.nombre }}</b>
                            @if (l.producto.presentacion) { <span class="muted">{{ l.producto.presentacion }}</span> }
                            @if (store.precios()) {
                              <span class="oline-sub">{{ cop(efectivo(l.producto) * l.cantidad) }}@if (l.cantidad > 1) {<small> ({{ cop(efectivo(l.producto)) }} cada uno)</small>}</span>
                            }
                          </div>
                          <div class="qty sm" role="group" [attr.aria-label]="'Cantidad de ' + l.producto.nombre">
                            <button type="button" [attr.aria-label]="l.cantidad === 1 ? 'Quitar ' + l.producto.nombre : 'Uno menos'"
                                    (click)="store.setCantidad(l.producto.id, l.cantidad - 1)">
                              <svg [catIcono]="l.cantidad === 1 ? 'trash' : 'minus'"></svg>
                            </button>
                            <output>{{ l.cantidad }}</output>
                            <button type="button" aria-label="Uno más" [disabled]="l.cantidad >= 99"
                                    (click)="store.setCantidad(l.producto.id, l.cantidad + 1)"><svg catIcono="plus"></svg></button>
                          </div>
                        </li>
                      }
                    </ul>
                  }
                </div>
              </div>
              @if (store.unidades()) {
                <div class="order-foot">
                  <div id="o-sum">
                    @if (store.precios()) {
                      <div class="total"><span>Total</span><b>{{ cop(store.total()) }}</b></div>
                      <p class="hint">{{ store.negocio().nombre }} te confirma el total y el domicilio por WhatsApp.</p>
                    } @else {
                      <p class="hint">{{ store.negocio().nombre }} te confirma precios y domicilio por WhatsApp.</p>
                    }
                  </div>
                  <form class="o-form" id="o-form" novalidate (submit)="enviar($event, nombre.value, entrega.value)">
                    <div class="o-fields">
                      <label>Tu nombre (opcional)<input #nombre class="input" name="nombre" autocomplete="name"></label>
                      <label>Dirección, o «paso a recoger» (opcional)<input #entrega class="input" name="entrega" autocomplete="street-address"></label>
                    </div>
                    @if (store.hayWhatsapp()) {
                      <button class="btn btn-brand"><svg catIcono="wa"></svg>Enviar pedido por WhatsApp</button>
                    } @else {
                      <p class="hint">{{ store.negocio().nombre }} todavía no tiene WhatsApp en su catálogo.</p>
                    }
                    <button type="button" class="btn link" (click)="store.vaciar()">Vaciar pedido</button>
                  </form>
                </div>
              }
            }

            @case ('menu') {
              <div class="sheet-scroll">
                <nav class="menu" aria-label="Secciones">
                  @for (m of menu(); track m.clave) {
                    <a [routerLink]="m.ruta" [queryParams]="m.query" (click)="cerrar()">{{ m.texto }}</a>
                  }
                </nav>
                <div class="menu-foot">
                  @if (store.hayWhatsapp()) {
                    <a class="btn btn-brand" [href]="store.hrefWhatsapp()" target="_blank" rel="noopener" (click)="elegirNumero($event)"><svg catIcono="wa"></svg>Escribir por WhatsApp</a>
                  }
                  <button type="button" class="btn" (click)="store.compartir()"><svg catIcono="share"></svg>Compartir catálogo</button>
                  <img class="ec" src="assets/images/brand/logo.svg" alt="Agorá Catalog" width="123" height="26">
                </div>
              </div>
            }

            @case ('filtros') {
              <div class="sheet-scroll fsheet">
                <h2>Filtros</h2>
                <div class="f-body"><cat-filtros /></div>
              </div>
              <div class="sheet-cta">
                <button type="button" class="btn btn-brand" (click)="cerrar()">Ver {{ plural(store.resultados().length, 'resultado') }}</button>
              </div>
            }

            @case ('whatsapp') {
              <div class="sheet-scroll m-pad">
                <h2>¿A qué número le escribes?</h2>
                <p class="muted">El mensaje ya va escrito.</p>
                <div class="wa-list">
                  @for (w of store.whatsapps(); track w.numero) {
                    <a class="btn btn-brand" [href]="waHref(w.numero, h.mensaje)" target="_blank" rel="noopener" (click)="store.registrar(h.pedido ? 'PEDIDO' : 'WHATSAPP'); cerrar()">
                      <svg catIcono="wa"></svg>
                      <span>@if (w.nombre) { {{ w.nombre }}<small>{{ phone(w.numero) }}</small> } @else { {{ phone(w.numero) }} }</span>
                    </a>
                  }
                </div>
              </div>
            }
          }
        </div>
      </div>
    }
  `
})
export class HojasComponent {
  readonly store = inject(CatalogoStore);
  readonly cop = cop;
  readonly plural = plural;
  readonly phone = phone;
  readonly efectivo = efectivo;
  readonly waHref = waHref;
  readonly msgProducto = msgProducto;
  readonly menu = computed(() => menuCatalogo(this.store));
  private readonly cerrarBtn = viewChild<ElementRef<HTMLButtonElement>>('cerrarBtn');
  private anterior: HTMLElement | null = null;

  readonly producto = computed(() => {
    const h = this.store.hoja();
    return h?.tipo === 'producto' ? this.store.porId().get(h.id) ?? null : null;
  });

  readonly titulo = computed(() => {
    const h = this.store.hoja();
    return h?.tipo === 'producto' ? this.producto()?.nombre ?? '' : h?.tipo === 'pedido' ? 'Mi pedido'
      : h?.tipo === 'menu' ? 'Menú' : h?.tipo === 'filtros' ? 'Filtros' : 'Elegir número de WhatsApp';
  });

  constructor() {
    // Cada producto abierto cuenta para "los más vistos" del dueño (una vez por sesión).
    effect(() => {
      const h = this.store.hoja();
      if (h?.tipo === 'producto') this.store.registrar('PRODUCTO', h.id);
    });
    // Al abrir: el foco va a "Cerrar" y la página de atrás no se desplaza. Al cerrar: vuelve a donde estaba.
    afterRenderEffect(() => {
      const abierta = !!this.store.hoja(), btn = this.cerrarBtn()?.nativeElement;
      document.body.style.overflow = abierta ? 'hidden' : '';
      if (abierta && btn && !btn.closest('.sheet')?.contains(document.activeElement)) {
        this.anterior ??= document.activeElement as HTMLElement | null;
        btn.focus();
      }
      if (!abierta && this.anterior) {
        this.anterior.focus?.({preventScroll: true});
        this.anterior = null;
      }
    });
  }

  cerrar(): void {
    this.store.hoja.set(null);
  }

  elegirNumero(e: Event, mensaje?: string): void {
    if (this.store.whatsapps().length > 1) {
      e.preventDefault();
      this.store.escribir(mensaje);
    }
  }

  enviar(e: Event, nombre: string, entrega: string): void {
    e.preventDefault();
    if (!this.store.unidades() || !this.store.hayWhatsapp()) return;
    const msg = mensajePedido(this.store.lineas(), this.store.precios(), nombre.trim(), entrega.trim());
    if (this.store.whatsapps().length > 1) {
      this.store.hoja.set({tipo: 'whatsapp', mensaje: msg, pedido: true});
      return;
    }
    this.store.registrar('PEDIDO');
    window.open(waHref(this.store.whatsapps()[0].numero, msg), '_blank', 'noopener');
    this.store.avisar('Abrimos WhatsApp con tu pedido');
  }
}
