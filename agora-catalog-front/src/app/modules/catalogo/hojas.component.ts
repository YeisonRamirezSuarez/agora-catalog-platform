import {afterRenderEffect, Component, computed, effect, ElementRef, inject, signal, viewChild} from '@angular/core';
import {RouterLink} from '@angular/router';

import {CatalogoStore} from './catalogo.store';
import {conPresentacion, cop, costoDomicilio, efectivo, mensajePedido, msgProducto, phone, plural, waHref} from './catalogo.util';
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
                  <span class="pc-badge"><cat-etiqueta [p]="elegido()!" /></span>
                  <cat-foto clase="sheet-ph" [src]="p.imagenUrl ?? p.imagenThumbUrl" />
                  <div class="ps-body">
                    <h2>{{ p.nombre }}</h2>
                    @if (opciones().length > 1) {
                      <div class="ps-vars" role="radiogroup" aria-label="Presentación">
                        @for (o of opciones(); track o.presentacion; let i = $index) {
                          <label><input type="radio" name="presentacion" [checked]="i === variante()" (change)="variante.set(i)"><b>{{ o.presentacion }}</b>@if (store.precios()) {<small>{{ cop(o.precioFinal ?? 0) }}</small>}</label>
                        }
                      </div>
                    } @else if (p.presentacion) { <p class="muted">{{ p.presentacion }}</p> }
                    @if (p.disponible) { <cat-precio [p]="elegido()!" clase="price big" /> }
                    @if (p.descripcion) { <p class="ps-desc">{{ p.descripcion }}</p> }
                    <button type="button" class="ps-share" (click)="store.compartir(p)"><svg catIcono="share"></svg>Compartir este producto</button>
                  </div>
                </div>
                @if ((store.pedidos() && p.disponible) || store.hayWhatsapp()) {
                  <div class="ps-foot">
                    @if (store.pedidos() && p.disponible) {
                      <div class="ps-buy">
                        <cat-cantidad clase="big" #cant />
                        <button type="button" class="btn btn-brand" (click)="store.agregar(elegido()!, cant.n()); cerrar()"><svg catIcono="cart"></svg>Agregar al pedido</button>
                      </div>
                    }
                    @if (store.hayWhatsapp()) {
                      <a [class]="store.pedidos() && p.disponible ? 'ps-ask' : 'btn btn-brand ps-ask-btn'" [href]="store.hrefWhatsapp(msgProducto(elegido()!))"
                         target="_blank" rel="noopener" (click)="elegirNumero($event, msgProducto(elegido()!))">
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
                      @for (l of store.lineas(); track l.clave) {
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
                                    (click)="store.setCantidad(l.clave, l.cantidad - 1)">
                              <svg [catIcono]="l.cantidad === 1 ? 'trash' : 'minus'"></svg>
                            </button>
                            <output>{{ l.cantidad }}</output>
                            <button type="button" aria-label="Uno más" [disabled]="l.cantidad >= 99"
                                    (click)="store.setCantidad(l.clave, l.cantidad + 1)"><svg catIcono="plus"></svg></button>
                          </div>
                        </li>
                      }
                    </ul>
                  }
                </div>
              </div>
              @if (store.unidades()) {
                <div class="order-foot">
                  @if (store.negocio().domicilioActivo) {
                    <div class="o-modo" role="radiogroup" aria-label="¿Cómo recibes tu pedido?">
                      <label><input type="radio" name="modo" value="domicilio" [checked]="modo() === 'domicilio'" (change)="modo.set('domicilio')"><svg catIcono="moto"></svg>Domicilio</label>
                      <label><input type="radio" name="modo" value="recoger" [checked]="modo() === 'recoger'" (change)="modo.set('recoger')"><svg catIcono="store"></svg>Recoger en la tienda</label>
                    </div>
                  }
                  <div id="o-sum">
                    @if (store.precios()) {
                      @if (envio() !== undefined) {
                        @if (envio() !== null) { <div class="o-linea"><span>Subtotal</span><span>{{ cop(store.total()) }}</span></div> }
                        <div class="o-linea"><span>Domicilio</span><span>{{ envio() === null ? 'Por confirmar' : envio() ? cop(envio()!) : 'Gratis' }}</span></div>
                      }
                      <div class="total"><span>Total</span><b>{{ cop(store.total() + (envio() ?? 0)) }}</b></div>
                      @if (falta()) {
                        <p class="o-aviso">El pedido mínimo para domicilio es {{ cop(store.negocio().domicilioMinimo!) }}: te faltan {{ cop(falta()) }}. También puedes recogerlo en la tienda.</p>
                      } @else if (gratisFalta()) {
                        <p class="hint">Te faltan {{ cop(gratisFalta()) }} para el domicilio gratis.</p>
                      } @else {
                        <p class="hint">{{ store.negocio().nombre }} te confirma {{ store.negocio().domicilioActivo ? 'el pedido' : 'el total y el domicilio' }} por WhatsApp.</p>
                      }
                    } @else {
                      <p class="hint">{{ store.negocio().nombre }} te confirma precios y domicilio por WhatsApp.</p>
                    }
                    @if (store.horarioAhora(); as e) {
                      @if (!e.abierto) { <p class="o-aviso">Ahora está cerrado: puedes enviar tu pedido y te responden cuando abran ({{ e.texto }}).</p> }
                    }
                  </div>
                  <form class="o-form" id="o-form" novalidate (submit)="enviar($event)">
                    <div class="o-fields">
                      <label>Tu nombre (opcional)<input class="input" name="nombre" autocomplete="name"></label>
                      @if (!store.negocio().domicilioActivo) {
                        <label>Dirección, o «paso a recoger» (opcional)<input class="input" name="entrega" autocomplete="street-address"></label>
                      } @else if (modo() === 'domicilio') {
                        <label>Dirección para el domicilio<input #direccion class="input" name="entrega" autocomplete="street-address" required></label>
                      }
                    </div>
                    @if (store.hayWhatsapp()) {
                      <button class="btn btn-brand" [disabled]="falta() > 0"><svg catIcono="wa"></svg>Enviar pedido por WhatsApp</button>
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
  private readonly direccion = viewChild<ElementRef<HTMLInputElement>>('direccion');

  /** Con domicilio configurado, el cliente elige cómo recibe el pedido. */
  readonly modo = signal<'domicilio' | 'recoger'>('domicilio');
  readonly domicilio = computed(() => this.store.negocio().domicilioActivo && this.modo() === 'domicilio');
  /** undefined: sin línea de domicilio; null: lo confirma el negocio; 0: gratis. */
  readonly envio = computed(() => this.domicilio() ? costoDomicilio(this.store.negocio(), this.store.total()) : undefined);
  /** Lo que falta para el pedido mínimo del domicilio (sin precios visibles no se puede saber). */
  readonly falta = computed(() => {
    const min = this.store.negocio().domicilioMinimo, total = this.store.total();
    return this.domicilio() && this.store.precios() && min && total < min ? min - total : 0;
  });
  readonly gratisFalta = computed(() => {
    const n = this.store.negocio(), total = this.store.total();
    return this.domicilio() && n.domicilioCosto && n.domicilioGratisDesde && total < n.domicilioGratisDesde
      ? n.domicilioGratisDesde - total : 0;
  });
  private anterior: HTMLElement | null = null;

  readonly producto = computed(() => {
    const h = this.store.hoja();
    return h?.tipo === 'producto' ? this.store.porId().get(h.id) ?? null : null;
  });

  /** La principal y las otras presentaciones; la elegida es la que se agrega y se pregunta. */
  readonly variante = signal(0);
  readonly opciones = computed(() => {
    const p = this.producto();
    return p ? [p, ...p.presentaciones.map(v => conPresentacion(p, v))] : [];
  });
  readonly elegido = computed(() => this.opciones()[this.variante()] ?? this.producto());

  readonly titulo = computed(() => {
    const h = this.store.hoja();
    return h?.tipo === 'producto' ? this.producto()?.nombre ?? '' : h?.tipo === 'pedido' ? 'Mi pedido'
      : h?.tipo === 'menu' ? 'Menú' : h?.tipo === 'filtros' ? 'Filtros' : 'Elegir número de WhatsApp';
  });

  constructor() {
    // Cada producto abierto cuenta para "los más vistos" del dueño (una vez por sesión).
    effect(() => {
      const h = this.store.hoja();
      this.variante.set(0);
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

  enviar(e: Event): void {
    e.preventDefault();
    if (!this.store.unidades() || !this.store.hayWhatsapp() || this.falta()) return;
    const datos = new FormData(e.target as HTMLFormElement);
    const nombre = String(datos.get('nombre') ?? '').trim(), entrega = String(datos.get('entrega') ?? '').trim();
    if (this.domicilio() && !entrega) {
      this.direccion()?.nativeElement.focus();
      this.store.avisar('Escribe la dirección para el domicilio');
      return;
    }
    const msg = mensajePedido(this.store.lineas(), this.store.precios(), {
      nombre, envio: this.envio(),
      entrega: !this.store.negocio().domicilioActivo ? entrega : this.domicilio() ? `domicilio a ${entrega}` : 'recojo en la tienda'
    });
    if (this.store.whatsapps().length > 1) {
      this.store.hoja.set({tipo: 'whatsapp', mensaje: msg, pedido: true});
      return;
    }
    this.store.registrar('PEDIDO');
    window.open(waHref(this.store.whatsapps()[0].numero, msg), '_blank', 'noopener');
    this.store.avisar('Abrimos WhatsApp con tu pedido');
  }
}
