import {Component, computed, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {debounceTime, Subject, switchMap} from 'rxjs';

import {apiMessage, AuthService} from '@app/core/services/auth.service';
import {PanelShellComponent} from '@app/shared/layouts/panel-shell/panel-shell.component';
import {NegocioService} from '@app/core/services/negocio.service';
import {ADMIN_LINKS, cop} from '../admin.service';
import {
  abrevUnidad, cantidad, Carro, CarroDetalle, enlaceWhatsapp, FRACCIONABLES, leerCantidad, numeroPedido, RutaService, StockItem,
  telefonoValido, textoComprobante
} from './ruta.service';

/** Una línea del pedido que se está armando. */
interface Linea {
  item: StockItem;
  clave: string;
  cantidad: number | null;
  texto: string;
  precio: number;
  subtotal: number;
  error: string;
}

const ULTIMO_CARRO = 'agora.pedido.carro';
const ENVIAR_WHATSAPP = 'agora.pedido.whatsapp';

/**
 * Venderle a una tienda desde un carro: se elige el carro, se escribe el nombre de la tienda (solo
 * queda en el pedido), se marcan las cantidades de lo que hay en el carro y se confirma; si se marca,
 * el comprobante sale por WhatsApp al número que se escriba (o al contacto que se escoja en WhatsApp).
 */
@Component({
  selector: 'app-nuevo-pedido',
  imports: [PanelShellComponent, FormsModule, RouterLink],
  template: `
    <app-panel-shell [subtitle]="empresa()" [links]="links">
      <header>
        @if (editandoId()) {
          <a class="btn-link text-sm" [routerLink]="['/admin/pedidos', editandoId()]">← Volver al comprobante</a>
          <h1 class="page-title mt-3">Editar pedido {{ numeroOriginal() }}</h1>
          <p class="mt-1 max-w-2xl text-muted">Cambia cantidades, precios, productos o la tienda. Al guardar, el carro se reacomoda: lo que quites vuelve al carro y lo que agregues se descuenta.</p>
        } @else {
          <h1 class="page-title">Nuevo pedido</h1>
          <p class="mt-1 max-w-2xl text-muted">Lo que le dejas a una tienda. Al confirmar se descuenta del carro y te queda el comprobante.</p>
        }
      </header>

      @if (error()) { <p class="alert-error mt-6" role="alert">{{ error() }}</p> }

      @if (!cargandoCarros() && !carros().length) {
        <div class="card mt-6 grid justify-items-start gap-3 p-6">
          <p>Para hacer un pedido necesitas un carro cargado con lo que llevas.</p>
          <a class="btn btn-dark" routerLink="/admin/carros">Ir a Carros</a>
        </div>
      } @else {
        <div class="mt-6 grid gap-4 sm:grid-cols-2">
          <div class="field">
            <label for="p-carro">Carro</label>
            <select class="input" id="p-carro" [value]="carroId() ?? ''" [disabled]="!!editandoId()" (change)="elegirCarro(+$any($event.target).value)">
              @if (!carroId()) { <option value="">Elige el carro</option> }
              @for (c of carros(); track c.id) {
                <option [value]="c.id" [selected]="c.id === carroId()">{{ c.nombre }}{{ c.productos ? '' : ' (vacío)' }}</option>
              }
            </select>
          </div>

          <div class="field relative">
            <label for="p-tienda">Tienda</label>
            <input class="input" id="p-tienda" autocomplete="off" maxlength="80" placeholder="Nombre de la tienda"
                   [value]="tiendaTexto()" (input)="escribirTienda($any($event.target).value)"
                   (focus)="mostrarSugerencias.set(true)" (blur)="ocultarSugerencias()">
            @if (mostrarSugerencias() && sugerencias().length) {
              <ul class="card absolute inset-x-0 top-full z-20 mt-1 grid gap-0.5 p-1 shadow-lg" aria-label="Tiendas de pedidos anteriores">
                @for (t of sugerencias(); track t) {
                  <li>
                    <button type="button" class="w-full rounded-lg px-3 py-2 text-left font-semibold hover:bg-wash" (mousedown)="elegirTienda(t)">{{ t }}</button>
                  </li>
                }
              </ul>
            }
          </div>
        </div>

        @if (carro(); as c) {
          <div class="mt-6 flex flex-wrap items-end justify-between gap-3">
            <h2 class="text-lg font-semibold">Lo que hay en {{ c.nombre }}</h2>
            @if (items().length > 6) {
              <input class="input sm:w-64" type="search" placeholder="Buscar en el carro" aria-label="Buscar en el carro"
                     [value]="filtro()" (input)="filtro.set($any($event.target).value)">
            }
          </div>

          <ul class="card mt-3 divide-y divide-line">
            @for (s of visibles(); track clave(s)) {
              <li class="grid grid-cols-[1fr_auto] items-center gap-3 px-4 py-3" [class.bg-wash]="!!texto(s)">
                <div class="min-w-0">
                  <p class="truncate font-semibold">{{ s.nombre }}</p>
                  <p class="truncate text-xs text-muted">
                    @if (s.presentacion) { {{ s.presentacion }} · }{{ cop(s.precio) }} · {{ editandoId() ? 'puedes poner hasta' : 'quedan' }} {{ cant(disponible(s)) }} {{ abrev(s) }}
                  </p>
                </div>
                @if (!texto(s)) {
                  <button type="button" class="btn min-h-11 min-w-11 px-3 text-lg" [attr.aria-label]="'Agregar ' + s.nombre" (click)="sumar(s, 1)">+</button>
                } @else {
                  <div class="flex items-center gap-1">
                    <button type="button" class="btn min-h-11 min-w-11 px-0 text-lg" [attr.aria-label]="'Quitar uno de ' + s.nombre" (click)="sumar(s, -1)">−</button>
                    <input class="input min-h-11 w-16 px-1 text-center tabular-nums" [attr.inputmode]="fraccion(s) ? 'decimal' : 'numeric'"
                           [attr.aria-label]="'Cantidad de ' + s.nombre" [class.border-danger]="!!errorDe(s)"
                           [value]="texto(s)" (input)="escribir(s, $any($event.target).value)">
                    <button type="button" class="btn min-h-11 min-w-11 px-0 text-lg" [attr.aria-label]="'Sumar uno de ' + s.nombre" (click)="sumar(s, 1)">+</button>
                  </div>
                }
                @if (errorDe(s); as e) { <p class="col-span-2 text-xs text-danger">{{ e }}</p> }
              </li>
            } @empty {
              <li class="px-4 py-10 text-center text-muted">
                @if (items().length) { Nada coincide con «{{ filtro() }}». }
                @else { Este carro está vacío. <a [routerLink]="['/admin/carros', c.id]">Cárgalo primero</a>. }
              </li>
            }
          </ul>
          <!-- Espacio para que la barra de abajo no tape el último producto. -->
          <div class="h-24"></div>
        }
      }

      @if (lineas().length) {
        <div class="fixed inset-x-0 bottom-0 z-40 border-t border-line bg-white px-4 py-3 shadow-[0_-4px_16px_rgba(0,0,0,0.06)] md:left-[240px]">
          <div class="mx-auto flex max-w-6xl items-center justify-between gap-3">
            <span>
              <span class="block text-sm text-muted">{{ lineas().length === 1 ? '1 producto' : lineas().length + ' productos' }}</span>
              <span class="text-xl font-semibold tabular-nums">{{ cop(total()) }}</span>
            </span>
            <button type="button" class="btn btn-dark" (click)="revisar()">Revisar pedido</button>
          </div>
        </div>
      }

      @if (revisando()) {
        <div class="fixed inset-0 z-50 flex items-end justify-center bg-black/50 sm:items-start sm:p-4 sm:pt-[6vh]" (click)="$event.target === $event.currentTarget ? revisando.set(false) : null">
          <form class="card flex max-h-[92dvh] w-full max-w-xl flex-col gap-4 overflow-y-auto [&>*]:shrink-0 rounded-b-none p-5 sm:max-h-[88dvh] sm:rounded-b-2xl sm:p-6"
                role="dialog" aria-modal="true" aria-label="Revisar pedido" (ngSubmit)="confirmar()">
            <div>
              <h2 class="text-xl font-semibold">Pedido para {{ tiendaTexto().trim() }}</h2>
              <p class="text-sm text-muted">Desde {{ carro()?.nombre }}. Puedes cambiar el precio de una línea si a esta tienda le cobras distinto.</p>
            </div>
            @if (errorConfirmar()) { <p class="alert-error" role="alert">{{ errorConfirmar() }}</p> }

            <ul class="grid gap-2">
              @for (l of lineas(); track l.clave) {
                <li class="grid grid-cols-[1fr_auto] items-center gap-x-3 gap-y-1 rounded-xl bg-wash px-3 py-2">
                  <span class="min-w-0">
                    <span class="block truncate font-semibold">{{ l.item.nombre }}</span>
                    <span class="block text-xs text-muted">{{ cant(l.cantidad ?? 0) }} {{ abrev(l.item) }}@if (l.item.presentacion) { · {{ l.item.presentacion }}}</span>
                  </span>
                  <span class="text-right font-semibold tabular-nums">{{ cop(l.subtotal) }}</span>
                  <label class="col-span-2 flex items-center gap-2 text-xs text-muted">
                    Precio c/u
                    <input class="input min-h-9 w-28 bg-white text-right tabular-nums" inputmode="numeric"
                           [attr.aria-label]="'Precio de ' + l.item.nombre" [class.border-ink]="l.precio !== l.item.precio"
                           [value]="miles(l.precio)" (input)="cambiarPrecio(l, $any($event.target).value)">
                    @if (l.precio !== l.item.precio) {
                      <button type="button" class="btn-link text-xs" (click)="cambiarPrecio(l, '' + l.item.precio)">catálogo {{ cop(l.item.precio) }}</button>
                    }
                  </label>
                </li>
              }
            </ul>

            <div class="flex items-center justify-between border-t border-line pt-3">
              <span class="font-semibold">Total</span>
              <span class="text-2xl font-semibold tabular-nums">{{ cop(total()) }}</span>
            </div>

            <div class="field"><label for="p-not">Notas (opcional)</label>
              <input class="input" id="p-not" name="notas" maxlength="300" [(ngModel)]="notas" placeholder="Paga el viernes, dejar con el vecino…"></div>

            <div class="grid gap-3 rounded-xl border border-line p-3">
              <label class="inline-flex items-center gap-2 font-semibold">
                <input type="checkbox" class="switch" role="switch" name="whatsapp" [ngModel]="enviarWhatsapp()" (ngModelChange)="marcarWhatsapp($event)">
                Enviar el comprobante por WhatsApp
              </label>
              @if (enviarWhatsapp()) {
                <div class="field">
                  <label for="p-tel">Número de WhatsApp (opcional)</label>
                  <input class="input" id="p-tel" name="telefono" type="tel" inputmode="tel" maxlength="20" autocomplete="off"
                         placeholder="300 123 4567" [(ngModel)]="telefono" [class.border-danger]="!telefonoOk()">
                  <p class="hint">{{ telefonoOk() ? 'Si lo dejas vacío, WhatsApp te deja escoger a quién mandarlo.' : 'Revisa el número: deben ser 10 dígitos.' }}</p>
                </div>
              }
            </div>

            <div class="flex flex-wrap gap-2">
              <button class="btn btn-dark" [disabled]="guardando()">{{ guardando() ? 'Guardando…' : editandoId() ? 'Guardar cambios' : 'Confirmar pedido' }}</button>
              <button type="button" class="btn-link" (click)="revisando.set(false)">Seguir agregando</button>
            </div>
          </form>
        </div>
      }
    </app-panel-shell>
  `
})
export class NuevoPedidoComponent implements OnInit {

  private readonly api = inject(RutaService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly links = ADMIN_LINKS;
  readonly cop = cop;
  readonly cant = cantidad;

  private readonly negocio = inject(NegocioService);
  readonly empresa = computed(() => this.auth.usuario()?.empresaNombre ?? 'Panel del negocio');
  readonly carros = signal<Carro[]>([]);
  readonly cargandoCarros = signal(true);
  readonly carroId = signal<number | null>(null);
  readonly carro = signal<CarroDetalle | null>(null);
  readonly filtro = signal('');
  readonly error = signal('');

  readonly tiendaTexto = signal('');
  readonly sugerencias = signal<string[]>([]);
  readonly mostrarSugerencias = signal(false);
  private readonly buscarTienda$ = new Subject<string>();

  /** Cantidad escrita por producto (clave -> texto) y precio cambiado a mano (clave -> precio). */
  readonly cantidades = signal<Map<string, string>>(new Map());
  readonly precios = signal<Map<string, number>>(new Map());

  readonly revisando = signal(false);
  readonly guardando = signal(false);
  readonly errorConfirmar = signal('');
  notas = '';

  /** Mandar el comprobante por WhatsApp al confirmar (se recuerda la última elección) y a qué número. */
  readonly enviarWhatsapp = signal(this.leerWhatsapp());
  telefono = '';

  /** Editando un pedido ya hecho: su id, su número y lo que tenía cada producto (clave -> cantidad). */
  readonly editandoId = signal<number | null>(null);
  readonly numeroOriginal = signal('');
  private readonly originales = signal<Map<string, number>>(new Map());
  /** Productos del pedido que ya no están en el carro (se vendieron todos): se siguen pudiendo editar. */
  private readonly delPedido = signal<StockItem[]>([]);

  /** Lo que se puede poner en el pedido: lo del carro y, al editar, lo que ya tenía el pedido. */
  readonly items = computed<StockItem[]>(() => {
    const stock = this.carro()?.stock ?? [];
    const enStock = new Set(stock.map(s => this.clave(s)));
    return [...stock, ...this.delPedido().filter(s => !enStock.has(this.clave(s)))];
  });

  readonly visibles = computed(() => {
    const q = this.normal(this.filtro());
    const stock = this.items();
    return q ? stock.filter(s => this.normal(s.nombre + ' ' + s.codigo + ' ' + (s.presentacion ?? '')).includes(q)) : stock;
  });

  readonly lineas = computed<Linea[]>(() => this.items().flatMap(item => {
    const clave = this.clave(item);
    const texto = this.cantidades().get(clave);
    if (!texto) return [];
    const n = leerCantidad(texto);
    const precio = this.precios().get(clave) ?? item.precio;
    return [{
      item, clave, texto, precio, cantidad: n,
      subtotal: n === null ? 0 : Math.round(n * precio),
      error: this.validar(item, n)
    }];
  }));

  readonly total = computed(() => this.lineas().reduce((t, l) => t + l.subtotal, 0));

  constructor() {
    const sub = this.buscarTienda$.pipe(
      debounceTime(250),
      switchMap(q => this.api.nombresTiendas(q))
    ).subscribe({
      // Si lo escrito ya es exactamente una sugerencia, no hace falta mostrarla.
      next: ns => this.sugerencias.set(ns.filter(n => n !== this.tiendaTexto().trim())),
      error: () => this.sugerencias.set([])
    });
    inject(DestroyRef).onDestroy(() => sub.unsubscribe());
  }

  ngOnInit(): void {
    const editar = Number(this.route.snapshot.paramMap.get('id'));
    if (editar) {
      this.cargarParaEditar(editar);
      return;
    }
    this.api.carros().subscribe({
      next: cs => {
        const activos = cs.filter(c => c.activo);
        this.carros.set(activos);
        this.cargandoCarros.set(false);
        // El carro de la URL (desde el detalle del carro), si no el último usado, si no el único que haya.
        const pedido = Number(this.route.snapshot.queryParamMap.get('carro')) || this.leerUltimo();
        const elegido = activos.find(c => c.id === pedido) ?? (activos.length === 1 ? activos[0] : null);
        if (elegido) this.elegirCarro(elegido.id);
      },
      error: err => {
        this.cargandoCarros.set(false);
        this.error.set(apiMessage(err));
      }
    });
  }

  elegirCarro(id: number): void {
    if (!id) return;
    this.carroId.set(id);
    this.cantidades.set(new Map());
    this.precios.set(new Map());
    this.filtro.set('');
    this.carro.set(null);
    try { localStorage.setItem(ULTIMO_CARRO, String(id)); } catch { /* sin almacenamiento: no se recuerda */ }
    this.api.carro(id).subscribe({
      next: c => this.carro.set(c),
      error: err => this.error.set(apiMessage(err))
    });
  }

  /** Llena la pantalla con el pedido: su carro (fijo), su tienda, sus cantidades y sus precios. */
  private cargarParaEditar(id: number): void {
    this.editandoId.set(id);
    this.api.pedido(id).subscribe({
      next: p => {
        if (p.carroEliminado) {
          this.error.set('El carro de este pedido ya se eliminó: el pedido no se puede editar.');
          this.cargandoCarros.set(false);
          return;
        }
        this.numeroOriginal.set(numeroPedido(p.numero));
        this.tiendaTexto.set(p.tiendaNombre);
        this.notas = p.notas ?? '';
        this.carroId.set(p.carroId);
        this.carros.set([{id: p.carroId, nombre: p.carroNombre, placa: null, activo: true, productos: 1, valor: 0}]);
        this.cargandoCarros.set(false);
        const cantidades = new Map<string, string>(), originales = new Map<string, number>(), precios = new Map<string, number>();
        const propios: StockItem[] = [];
        for (const i of p.items) {
          if (i.productoId === null) continue;
          const item: StockItem = {productoId: i.productoId, codigo: '', nombre: i.productoNombre, presentacion: i.presentacion,
            unidadVenta: i.unidadVenta, cantidad: 0, precio: i.precioSugerido, imagenThumbUrl: null};
          const k = this.clave(item);
          originales.set(k, (originales.get(k) ?? 0) + i.cantidad);
          cantidades.set(k, cantidad(originales.get(k)!).replace(/\./g, ''));
          precios.set(k, i.precioUnitario);
          propios.push(item);
        }
        this.originales.set(originales);
        this.delPedido.set(propios);
        this.api.carro(p.carroId).subscribe({
          next: c => {
            this.carro.set(c);
            // El precio con que se vendió se conserva: solo queda marcado si difiere del de catálogo.
            const ajustados = new Map<string, number>();
            for (const s of this.items()) {
              const k = this.clave(s), cobrado = precios.get(k);
              if (cobrado !== undefined && cobrado !== s.precio) ajustados.set(k, cobrado);
            }
            this.precios.set(ajustados);
            this.cantidades.set(cantidades);
          },
          error: err => this.error.set(apiMessage(err))
        });
      },
      error: err => {
        this.cargandoCarros.set(false);
        this.error.set(apiMessage(err, 'No encontramos este pedido.'));
      }
    });
  }

  /** Cuánto se puede poner de un producto: lo que queda en el carro más lo que ya tenía este pedido. */
  disponible(s: StockItem): number {
    return s.cantidad + (this.originales().get(this.clave(s)) ?? 0);
  }

  // ---- Tienda ----

  escribirTienda(texto: string): void {
    this.tiendaTexto.set(texto);
    this.mostrarSugerencias.set(true);
    if (texto.trim()) this.buscarTienda$.next(texto.trim()); else this.sugerencias.set([]);
  }

  elegirTienda(nombre: string): void {
    this.tiendaTexto.set(nombre);
    this.sugerencias.set([]);
    this.mostrarSugerencias.set(false);
  }

  ocultarSugerencias(): void {
    // Después del mousedown de una sugerencia.
    setTimeout(() => this.mostrarSugerencias.set(false), 150);
  }

  // ---- Cantidades ----

  clave(s: StockItem): string {
    return s.productoId + '|' + (s.presentacion ?? '').toLowerCase();
  }

  texto(s: StockItem): string {
    return this.cantidades().get(this.clave(s)) ?? '';
  }

  fraccion(s: StockItem): boolean {
    return !s.presentacion && FRACCIONABLES.includes(s.unidadVenta);
  }

  abrev(s: StockItem): string {
    return abrevUnidad(s.unidadVenta, s.presentacion);
  }

  errorDe(s: StockItem): string {
    return this.lineas().find(l => l.clave === this.clave(s))?.error ?? '';
  }

  sumar(s: StockItem, delta: number): void {
    const actual = leerCantidad(this.texto(s)) ?? 0;
    const nueva = Math.max(0, Math.round((actual + delta) * 1000) / 1000);
    this.escribir(s, nueva ? cantidad(nueva).replace(/\./g, '') : '');
  }

  escribir(s: StockItem, texto: string): void {
    const m = new Map(this.cantidades());
    if (texto.trim()) m.set(this.clave(s), texto); else m.delete(this.clave(s));
    this.cantidades.set(m);
  }

  // ---- Revisar y confirmar ----

  revisar(): void {
    if (!this.tiendaTexto().trim()) {
      this.error.set('Escribe el nombre de la tienda antes de revisar el pedido.');
      document.getElementById('p-tienda')?.focus();
      return;
    }
    const conError = this.lineas().find(l => l.error);
    if (conError) {
      this.error.set(`Revisa «${conError.item.nombre}»: ${conError.error.toLowerCase()}`);
      return;
    }
    this.error.set('');
    this.errorConfirmar.set('');
    this.revisando.set(true);
  }

  miles(n: number): string {
    return cop(n).slice(1);
  }

  cambiarPrecio(l: Linea, texto: string): void {
    const digitos = texto.replace(/\D/g, '');
    const m = new Map(this.precios());
    const precio = digitos ? Math.min(Number(digitos), 999_999_999) : 0;
    if (precio === l.item.precio) m.delete(l.clave); else m.set(l.clave, precio);
    this.precios.set(m);
  }

  // ---- WhatsApp ----

  telefonoOk(): boolean {
    return telefonoValido(this.telefono);
  }

  marcarWhatsapp(si: boolean): void {
    this.enviarWhatsapp.set(si);
    try { localStorage.setItem(ENVIAR_WHATSAPP, si ? '1' : '0'); } catch { /* sin almacenamiento: no se recuerda */ }
  }

  private leerWhatsapp(): boolean {
    try { return localStorage.getItem(ENVIAR_WHATSAPP) === '1'; } catch { return false; }
  }

  confirmar(): void {
    const carroId = this.carroId();
    if (!carroId) return;
    const whatsapp = this.enviarWhatsapp();
    if (whatsapp && !this.telefonoOk()) {
      this.errorConfirmar.set('Revisa el número de WhatsApp: deben ser 10 dígitos, o déjalo vacío para escoger el contacto.');
      return;
    }
    // La pestaña de WhatsApp se abre ya, con el toque: abierta después de guardar, el navegador la bloquearía.
    const ventana = whatsapp ? window.open('', '_blank') : null;
    this.guardando.set(true);
    this.errorConfirmar.set('');
    const editando = this.editandoId();
    const datos = {
      tiendaNombre: this.tiendaTexto().trim(),
      notas: this.notas.trim() || null,
      items: this.lineas().map(l => ({
        productoId: l.item.productoId, presentacion: l.item.presentacion, cantidad: l.cantidad!,
        // Al editar el precio va siempre: el de catálogo pudo cambiar desde que se hizo el pedido.
        precioUnitario: editando || l.precio !== l.item.precio ? l.precio : null
      }))
    };
    (editando ? this.api.editarPedido(editando, datos) : this.api.crearPedido({carroId, ...datos})).subscribe({
      next: p => {
        const queryParams: Record<string, string | number> = editando ? {editado: 1} : {nuevo: 1};
        if (whatsapp) {
          const enlace = enlaceWhatsapp(textoComprobante(p, this.negocio.negocio()?.nombre ?? this.empresa(), cop), this.telefono);
          // Si el navegador no dejó abrir la pestaña, el comprobante abre la ventana de WhatsApp con el número.
          if (ventana) ventana.location.href = enlace; else Object.assign(queryParams, {whatsapp: 1, tel: this.telefono});
        }
        void this.router.navigate(['/admin/pedidos', p.id], {queryParams});
      },
      error: err => {
        ventana?.close();
        this.guardando.set(false);
        this.errorConfirmar.set(apiMessage(err));
        // Otro pedido pudo descontar del carro mientras tanto: se trae el stock al día.
        this.api.carro(carroId).subscribe({next: c => this.carro.set(c), error: () => undefined});
      }
    });
  }

  private validar(s: StockItem, n: number | null): string {
    if (n === null || n <= 0) return 'Escribe una cantidad mayor que cero';
    if (!this.fraccion(s) && !Number.isInteger(n)) return 'Va en cantidades enteras';
    if (n > this.disponible(s)) return `Solo puedes poner hasta ${cantidad(this.disponible(s))} ${this.abrev(s)}`;
    return '';
  }

  private leerUltimo(): number {
    try { return Number(localStorage.getItem(ULTIMO_CARRO)) || 0; } catch { return 0; }
  }

  private normal(s: string): string {
    return s.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').trim();
  }
}
