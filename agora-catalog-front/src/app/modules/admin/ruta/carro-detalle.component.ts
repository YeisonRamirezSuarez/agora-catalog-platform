import {Component, computed, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {debounceTime, Subject, switchMap} from 'rxjs';

import {apiMessage, AuthService} from '@app/core/services/auth.service';
import {PanelShellComponent} from '@app/shared/layouts/panel-shell/panel-shell.component';
import {ADMIN_LINKS, AdminService, cop, Producto, UnidadVenta} from '../admin.service';
import {
  abrevUnidad, cantidad, CarroDetalle, CarroForm, dia, FRACCIONABLES, hora, leerCantidad, numeroPedido, RutaCarro, RutaItem, RutaService,
  StockItem
} from './ruta.service';

/** Una línea de la carga que se está armando (la cantidad es el texto tal como se escribe). */
interface LineaCarga {
  clave: string;
  productoId: number;
  nombre: string;
  presentacion: string | null;
  etiqueta: string | null;
  fraccionable: boolean;
  texto: string;
}

/** Una opción para agregar a la carga: el producto en su presentación principal o en otra. */
interface Opcion {
  producto: Producto;
  presentacion: string | null;
  etiqueta: string;
  precio: number;
}

/** Las rutas de un mismo día, juntas. */
interface DiaDeRutas {
  dia: string;
  rutas: RutaCarro[];
}

const RUTAS_POR_PAGINA = 10;

/** Un carro: lo que lleva, agregar, cambiar o quitar productos, sus rutas por fecha y vaciarlo. */
@Component({
  selector: 'app-carro-detalle',
  imports: [PanelShellComponent, FormsModule, RouterLink],
  template: `
    <app-panel-shell [subtitle]="empresa()" [links]="links">
      <a class="btn-link text-sm" routerLink="/admin/carros">← Carros</a>

      @if (carro(); as c) {
        <header class="mt-3 flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 class="page-title">{{ c.nombre }}</h1>
            <p class="mt-1 text-muted">
              @if (c.placa) { <span class="font-mono">{{ c.placa }}</span> · }
              {{ c.productos === 1 ? '1 producto' : c.productos + ' productos' }} · {{ cop(c.valor) }} a precio de catálogo
              @if (!c.activo) { · <span class="badge bg-wash text-muted">Inactivo</span> }
            </p>
            @if (rutaActual(); as r) {
              <p class="mt-1 text-sm font-semibold text-ok-ink">Ruta {{ r.numero }} de {{ diaMin(r.iniciadaAt) }} · salió a las {{ hora(r.iniciadaAt) }}</p>
            }
          </div>
          <button type="button" class="btn min-h-10 px-3 text-sm" (click)="abrirEditar()">Editar carro</button>
        </header>

        @if (error()) { <p class="alert-error mt-6" role="alert">{{ error() }}</p> }
        @if (aviso()) { <p class="alert-ok mt-6" role="status">{{ aviso() }}</p> }

        <div class="mt-6 grid grid-cols-2 gap-2 sm:flex">
          @if (c.stock.length && c.activo) {
            <a class="btn btn-dark col-span-2" routerLink="/admin/pedidos/nuevo" [queryParams]="{carro: c.id}">Vender a una tienda</a>
          }
          <button type="button" class="btn" [class.btn-dark]="!c.stock.length" [class.col-span-2]="!puedeNuevaRuta()"
                  [disabled]="!c.activo" (click)="abrirCarga()">+ Agregar productos</button>
          @if (puedeNuevaRuta()) {
            <button type="button" class="btn" [disabled]="!c.activo || ocupado()" (click)="nuevaRutaAbierta.set(true)">Empezar ruta nueva</button>
          }
        </div>
        @if (!c.activo) { <p class="hint mt-2">El carro está inactivo: actívalo en «Editar carro» para agregarle productos.</p> }

        <div class="mt-8 flex flex-wrap items-baseline justify-between gap-2">
          <h2 class="text-lg font-semibold">Lo que lleva</h2>
          @if (c.stock.length) {
            <button type="button" class="btn-link text-sm" [disabled]="ocupado()" (click)="descargar()">Vaciar carro</button>
          }
        </div>
        @if (c.stock.length) { <p class="mt-1 text-sm text-muted">Toca un producto para sumarle, restarle o quitarlo.</p> }
        <ul class="card mt-3 divide-y divide-line">
          @for (s of c.stock; track clave(s.productoId, s.presentacion)) {
            <li>
              @if (fila() !== clave(s.productoId, s.presentacion)) {
                <button type="button" class="grid w-full grid-cols-[44px_1fr_auto] items-center gap-3 px-4 py-3 text-left hover:bg-wash"
                        [attr.aria-label]="'Cambiar ' + s.nombre" (click)="editarFila(s)">
                  @if (s.imagenThumbUrl) {
                    <img [src]="s.imagenThumbUrl" alt="" class="size-11 rounded-lg object-cover" loading="lazy">
                  } @else {
                    <span class="grid size-11 place-items-center rounded-lg bg-wash text-xs text-faint" aria-hidden="true">—</span>
                  }
                  <span class="min-w-0">
                    <span class="block truncate font-semibold">{{ s.nombre }}</span>
                    <span class="block truncate text-xs text-muted">@if (s.presentacion) { {{ s.presentacion }} · }{{ cop(s.precio) }} c/u</span>
                  </span>
                  <span class="text-right">
                    <span class="block text-lg font-semibold tabular-nums">{{ cant(s.cantidad) }}</span>
                    <span class="block text-xs text-muted">{{ unidad(s) }}</span>
                  </span>
                </button>
              } @else {
                <div class="grid gap-3 bg-wash px-4 py-3">
                  <p class="font-semibold">{{ s.nombre }}@if (s.presentacion) { <span class="font-normal text-muted"> · {{ s.presentacion }}</span> }</p>
                  <div class="flex flex-wrap items-center gap-2">
                    <button type="button" class="btn min-h-11 min-w-11 bg-white px-0 text-lg" aria-label="Restar uno" (click)="sumarFila(-1)">−</button>
                    <input class="input min-h-11 w-20 bg-white px-1 text-center tabular-nums" [attr.inputmode]="fraccion(s) ? 'decimal' : 'numeric'"
                           [attr.aria-label]="'Cantidad de ' + s.nombre" [value]="textoFila()" (input)="textoFila.set($any($event.target).value)">
                    <button type="button" class="btn min-h-11 min-w-11 bg-white px-0 text-lg" aria-label="Sumar uno" (click)="sumarFila(1)">+</button>
                    <span class="text-sm text-muted">{{ unidad(s) }}</span>
                  </div>
                  @if (errorFila()) { <p class="text-sm text-danger" role="alert">{{ errorFila() }}</p> }
                  <div class="flex flex-wrap items-center gap-2">
                    <button type="button" class="btn btn-dark" [disabled]="ocupado()" (click)="guardarFila(s)">{{ ocupado() ? 'Guardando…' : 'Guardar' }}</button>
                    <button type="button" class="btn-link" (click)="fila.set(null)">Cancelar</button>
                    <span class="flex-1"></span>
                    <button type="button" class="btn-link text-danger" [disabled]="ocupado()" (click)="quitarFila(s)">Quitar del carro</button>
                  </div>
                </div>
              }
            </li>
          } @empty {
            <li class="px-4 py-10 text-center text-muted">El carro está vacío. Usa «Agregar productos» para llenarlo.</li>
          }
        </ul>

        <h2 class="mt-8 text-lg font-semibold">Rutas</h2>
        <p class="mt-1 text-sm text-muted">Lo que cargó, vendió y le quedó al carro en cada salida.</p>
        @for (d of dias(); track d.dia) {
          <h3 class="mt-5 text-sm font-semibold uppercase tracking-wide text-muted">{{ d.dia }}</h3>
          @for (r of d.rutas; track r.id) {
            <article class="card mt-2 p-4">
              <header class="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1">
                <p class="font-semibold">
                  Ruta {{ r.numero }}
                  <span class="font-normal text-muted">· {{ hora(r.iniciadaAt) }}@if (r.cerradaAt) { a {{ hora(r.cerradaAt) }} }</span>
                  @if (!r.cerradaAt) { <span class="badge ml-1 bg-ok-soft text-ok-ink">En curso</span> }
                </p>
                <p class="text-sm text-muted">{{ r.pedidos.length === 1 ? '1 pedido' : r.pedidos.length + ' pedidos' }}</p>
              </header>
              @if (r.items.length) {
                <table class="mt-3 w-full text-sm">
                  <thead>
                    <tr class="text-xs text-muted">
                      <th class="pb-1 text-left font-semibold">Producto</th>
                      <th class="w-14 pb-1 text-right font-semibold">Cargó</th>
                      <th class="w-14 pb-1 text-right font-semibold">Vendió</th>
                      <th class="w-14 pb-1 text-right font-semibold">{{ r.cerradaAt ? 'Quedó' : 'Lleva' }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (i of r.items; track $index) {
                      <tr class="border-t border-line align-top">
                        <td class="py-2 pr-2">
                          <span class="font-semibold">{{ i.nombre }}</span>
                          <span class="text-muted"> · {{ i.presentacion ?? abrevItem(i) }}</span>
                          @if (i.traia) { <span class="block text-xs text-muted">{{ cant(i.traia) }} venían de la ruta anterior</span> }
                          @if (i.quitado) { <span class="block text-xs text-muted">{{ cant(i.quitado) }} se bajaron del carro</span> }
                        </td>
                        <td class="py-2 text-right tabular-nums">{{ i.cargado ? cant(i.cargado) : '—' }}</td>
                        <td class="py-2 text-right tabular-nums">{{ i.vendido ? cant(i.vendido) : '—' }}</td>
                        <td class="py-2 text-right font-semibold tabular-nums">{{ cant(i.queda) }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              } @else {
                <p class="mt-2 text-sm text-muted">Todavía no tiene productos.</p>
              }
              @if (r.pedidos.length) {
                <button type="button" class="btn mt-3 min-h-10 w-full px-3 text-sm" [attr.aria-expanded]="abiertas().has(r.id)"
                        (click)="verPedidos(r.id)">
                  {{ abiertas().has(r.id) ? 'Ocultar pedidos' : 'Ver pedidos (' + r.pedidos.length + ')' }}
                </button>
              }
              @if (r.pedidos.length && abiertas().has(r.id)) {
                <ul class="mt-2 divide-y divide-line rounded-xl border border-line">
                  @for (p of r.pedidos; track p.id) {
                    <li>
                      <a class="grid grid-cols-[1fr_auto] items-center gap-x-3 px-3 py-2 text-sm no-underline hover:bg-wash"
                         [routerLink]="['/admin/pedidos', p.id]" [attr.aria-label]="'Ver pedido ' + numero(p.numero) + ' a ' + p.tiendaNombre">
                        <span class="min-w-0">
                          <span class="block truncate font-semibold">{{ p.tiendaNombre }}</span>
                          <span class="block text-xs text-muted">{{ numero(p.numero) }} · {{ hora(p.createdAt) }}@if (p.estado === 'ANULADO') { · anulado }</span>
                        </span>
                        <span class="font-semibold tabular-nums" [class.line-through]="p.estado === 'ANULADO'">{{ cop(p.total) }} ›</span>
                      </a>
                    </li>
                  }
                </ul>
                <p class="mt-2 flex justify-between text-sm">
                  <span class="text-muted">Vendido en esta ruta</span>
                  <b class="tabular-nums">{{ cop(vendidoEn(r)) }}</b>
                </p>
              }
            </article>
          }
        } @empty {
          <p class="card mt-3 px-4 py-8 text-center text-muted">Todavía no ha salido a ruta. Empieza agregándole productos.</p>
        }
        @if (hayMasRutas()) {
          <button type="button" class="btn mt-3 w-full" [disabled]="cargandoRutas()" (click)="masRutas()">
            {{ cargandoRutas() ? 'Cargando…' : 'Ver rutas anteriores' }}
          </button>
        }

        <section class="mt-10 grid gap-2 border-t border-line pt-6">
          <h2 class="font-semibold">Eliminar este carro</h2>
          @if (c.stock.length) {
            <p class="text-sm text-muted">Todavía lleva mercancía. Vacíalo primero («Vaciar carro») para poder eliminarlo.</p>
          } @else {
            <p class="text-sm text-muted">Desaparece de tus carros y de «Nuevo pedido». Los pedidos que hiciste desde él no se borran.</p>
          }
          <button type="button" class="btn justify-self-start text-danger" [disabled]="!!c.stock.length || ocupado()" (click)="eliminar()">Eliminar carro</button>
        </section>
      } @else {
        @if (error()) { <p class="alert-error mt-6" role="alert">{{ error() }}</p> }
        @else { <p class="mt-6 text-muted">Cargando…</p> }
      }

      <!-- Cargar productos: buscar, elegir presentación, cantidades y confirmar. -->
      @if (cargaAbierta()) {
        <!-- Alto fijo en celular y anclado arriba en computador: al agregar una línea, los resultados no se mueven bajo el dedo. -->
        <div class="fixed inset-0 z-50 flex items-end justify-center bg-black/50 sm:items-start sm:p-4 sm:pt-[6vh]" (click)="$event.target === $event.currentTarget ? cerrarCarga() : null">
          <form class="card flex h-[92dvh] w-full max-w-xl flex-col gap-4 overflow-y-auto [&>*]:shrink-0 rounded-b-none p-5 sm:h-auto sm:max-h-[88dvh] sm:rounded-b-2xl sm:p-6"
                role="dialog" aria-modal="true" aria-label="Agregar productos" (ngSubmit)="confirmarCarga()">
            <h2 class="text-xl font-semibold">Agregar productos a {{ carro()?.nombre }}</h2>
            @if (errorCarga()) { <p class="alert-error" role="alert">{{ errorCarga() }}</p> }

            <div class="field">
              <label for="c-bus">Buscar producto</label>
              <input class="input" id="c-bus" type="search" placeholder="Nombre o código" autocomplete="off"
                     [value]="busqueda()" (input)="buscar($any($event.target).value)">
            </div>
            @if (busqueda().trim()) {
              <ul class="-mt-2 grid max-h-56 gap-1 overflow-y-auto rounded-xl border border-line p-1" aria-label="Resultados">
                @for (o of opciones(); track o.producto.id + '|' + (o.presentacion ?? '')) {
                  <li>
                    <button type="button" class="flex w-full items-center justify-between gap-3 rounded-lg px-3 py-2 text-left hover:bg-wash"
                            (click)="agregar(o)">
                      <span class="min-w-0">
                        <span class="block truncate font-semibold">{{ o.producto.nombre }}</span>
                        <span class="block truncate text-xs text-muted">{{ o.etiqueta }} · {{ cop(o.precio) }}</span>
                      </span>
                      <span class="shrink-0 text-sm font-semibold">{{ yaEsta(o) ? '+1' : 'Agregar' }}</span>
                    </button>
                  </li>
                } @empty {
                  <li class="px-3 py-4 text-center text-sm text-muted">{{ buscando() ? 'Buscando…' : 'Ningún producto con ese nombre.' }}</li>
                }
              </ul>
            }

            @if (lineas().length) {
              <ul class="grid gap-2" aria-label="Productos a cargar">
                @for (l of lineas(); track l.clave) {
                  <li class="grid grid-cols-[1fr_auto_auto] items-center gap-2 rounded-xl bg-wash px-3 py-2">
                    <span class="min-w-0">
                      <span class="block truncate font-semibold">{{ l.nombre }}</span>
                      @if (l.etiqueta) { <span class="block truncate text-xs text-muted">{{ l.etiqueta }}</span> }
                    </span>
                    <input class="input min-h-10 w-24 bg-white text-right tabular-nums" [attr.inputmode]="l.fraccionable ? 'decimal' : 'numeric'"
                           [attr.aria-label]="'Cantidad de ' + l.nombre" [class.border-danger]="!valida(l)"
                           [value]="l.texto" (input)="cambiarTexto(l, $any($event.target).value)">
                    <button type="button" class="grid size-10 place-items-center rounded-lg text-muted hover:bg-white" [attr.aria-label]="'Quitar ' + l.nombre" (click)="quitar(l)">✕</button>
                  </li>
                }
              </ul>
              <div class="field"><label for="c-not">Nota (opcional)</label>
                <input class="input" id="c-not" name="nota" maxlength="200" [(ngModel)]="nota" placeholder="Salida del lunes"></div>
            } @else {
              <p class="text-sm text-muted">Busca los productos que vas a subir al carro y escribe cuántos llevas de cada uno.</p>
            }

            <div class="flex flex-wrap gap-2">
              <button class="btn btn-dark" [disabled]="ocupado() || !lineas().length || !todasValidas()">
                {{ ocupado() ? 'Guardando…' : !lineas().length ? 'Agregar' : lineas().length === 1 ? 'Agregar 1 producto' : 'Agregar ' + lineas().length + ' productos' }}
              </button>
              <button type="button" class="btn-link" (click)="cerrarCarga()">Cancelar</button>
            </div>
          </form>
        </div>
      }

      @if (nuevaRutaAbierta()) {
        <div class="fixed inset-0 z-50 grid place-items-end bg-black/50 sm:place-items-center sm:p-4" (click)="$event.target === $event.currentTarget ? nuevaRutaAbierta.set(false) : null">
          <div class="card grid max-h-[92dvh] w-full max-w-md gap-4 overflow-y-auto rounded-b-none p-5 sm:rounded-b-2xl sm:p-6" role="dialog" aria-modal="true" aria-label="Empezar ruta nueva">
            <h2 class="text-xl font-semibold">Empezar ruta nueva</h2>
            @if (errorRuta()) { <p class="alert-error" role="alert">{{ errorRuta() }}</p> }
            @if (carro()?.stock?.length) {
              <p>De la ruta anterior le quedaron estos productos al carro:</p>
              <ul class="grid max-h-60 gap-1 overflow-y-auto rounded-xl bg-wash p-3 text-sm">
                @for (s of carro()!.stock; track clave(s.productoId, s.presentacion)) {
                  <li class="flex justify-between gap-3">
                    <span class="min-w-0 truncate">{{ s.nombre }}@if (s.presentacion) { <span class="text-muted"> · {{ s.presentacion }}</span> }</span>
                    <span class="shrink-0 font-semibold tabular-nums">{{ cant(s.cantidad) }} {{ unidad(s) }}</span>
                  </li>
                }
              </ul>
              <p class="font-semibold">¿Los llevas en la ruta nueva?</p>
              <div class="grid gap-2">
                <button type="button" class="btn btn-dark" [disabled]="ocupado()" (click)="empezarRuta(true)">Sí, siguen en el carro</button>
                <button type="button" class="btn" [disabled]="ocupado()" (click)="empezarRuta(false)">No, bájalos del carro</button>
                <button type="button" class="btn-link justify-self-start" (click)="nuevaRutaAbierta.set(false)">Cancelar</button>
              </div>
            } @else {
              <p class="text-muted">El carro está vacío: la ruta nueva empieza desde cero y enseguida le agregas lo que va a llevar.</p>
              <div class="flex flex-wrap items-center gap-2">
                <button type="button" class="btn btn-dark" [disabled]="ocupado()" (click)="empezarRuta(true)">Empezar ruta</button>
                <button type="button" class="btn-link" (click)="nuevaRutaAbierta.set(false)">Cancelar</button>
              </div>
            }
          </div>
        </div>
      }

      @if (editando(); as f) {
        <div class="fixed inset-0 z-50 grid place-items-end bg-black/50 sm:place-items-center sm:p-4" (click)="$event.target === $event.currentTarget ? editando.set(null) : null">
          <form class="card grid w-full max-w-md gap-4 rounded-b-none p-5 sm:rounded-b-2xl sm:p-6" role="dialog" aria-modal="true" aria-label="Editar carro" (ngSubmit)="guardarCarro(f)">
            <h2 class="text-xl font-semibold">Editar carro</h2>
            @if (errorEditar()) { <p class="alert-error" role="alert">{{ errorEditar() }}</p> }
            <div class="field"><label for="e-nom">Nombre</label><input class="input" id="e-nom" name="nombre" maxlength="60" [(ngModel)]="f.nombre"></div>
            <div class="field"><label for="e-pla">Placa</label><input class="input" id="e-pla" name="placa" maxlength="15" [(ngModel)]="f.placa"></div>
            <label class="inline-flex items-center gap-2 font-semibold">
              <input type="checkbox" class="switch" role="switch" name="activo" [(ngModel)]="f.activo">Activo
            </label>
            <div class="flex flex-wrap items-center gap-2">
              <button class="btn btn-dark" [disabled]="ocupado() || !f.nombre.trim()">Guardar</button>
              <button type="button" class="btn-link" (click)="editando.set(null)">Cancelar</button>
              <span class="flex-1"></span>
              <button type="button" class="btn min-h-10 px-3 text-sm text-danger" (click)="eliminar()">Eliminar carro</button>
            </div>
          </form>
        </div>
      }
    </app-panel-shell>
  `
})
export class CarroDetalleComponent implements OnInit {

  private readonly api = inject(RutaService);
  private readonly admin = inject(AdminService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly links = ADMIN_LINKS;
  readonly cop = cop;
  readonly cant = cantidad;
  readonly hora = hora;
  readonly numero = numeroPedido;

  readonly empresa = computed(() => this.auth.usuario()?.empresaNombre ?? 'Panel del negocio');
  readonly carro = signal<CarroDetalle | null>(null);
  /** Rutas ya traídas (de la más reciente a la más vieja) y si quedan más por traer. */
  readonly rutas = signal<RutaCarro[]>([]);
  readonly hayMasRutas = signal(false);
  readonly cargandoRutas = signal(false);
  private paginaRutas = 0;

  /** Las rutas agrupadas por día. */
  readonly dias = computed<DiaDeRutas[]>(() => {
    const dias: DiaDeRutas[] = [];
    for (const r of this.rutas()) {
      const d = dia(r.iniciadaAt);
      if (dias.at(-1)?.dia === d) dias.at(-1)!.rutas.push(r);
      else dias.push({dia: d, rutas: [r]});
    }
    return dias;
  });
  readonly rutaActual = computed(() => this.rutas().find(r => !r.cerradaAt) ?? null);
  /** Hay algo que cerrar: la ruta que va tiene productos o el carro lleva algo. */
  readonly puedeNuevaRuta = computed(() => !!this.carro()?.stock.length || !!this.rutaActual()?.items.length);

  // Ruta nueva
  readonly nuevaRutaAbierta = signal(false);
  readonly errorRuta = signal('');
  /** Rutas con la lista de pedidos abierta (cerradas de entrada). */
  readonly abiertas = signal<Set<number>>(new Set());

  verPedidos(rutaId: number): void {
    this.abiertas.update(s => {
      const n = new Set(s);
      if (!n.delete(rutaId)) n.add(rutaId);
      return n;
    });
  }
  readonly error = signal('');
  readonly aviso = signal('');
  readonly ocupado = signal(false);

  // Carga
  readonly cargaAbierta = signal(false);
  readonly busqueda = signal('');
  readonly buscando = signal(false);
  readonly resultados = signal<Producto[]>([]);
  readonly lineas = signal<LineaCarga[]>([]);
  readonly errorCarga = signal('');
  private readonly buscar$ = new Subject<string>();
  nota = '';

  // Cambiar la cantidad de un producto del carro (una fila a la vez).
  readonly fila = signal<string | null>(null);
  readonly textoFila = signal('');
  readonly errorFila = signal('');

  // Editar
  readonly editando = signal<CarroForm | null>(null);
  readonly errorEditar = signal('');

  private id = 0;

  /** Cada producto encontrado, en su presentación principal y en las otras. */
  readonly opciones = computed<Opcion[]>(() => this.resultados().flatMap(p => [
    {producto: p, presentacion: null, etiqueta: p.presentacion || 'Presentación principal', precio: p.precio},
    ...p.presentaciones.map(v => ({producto: p, presentacion: v.nombre, etiqueta: v.nombre, precio: v.precio}))
  ]));

  constructor() {
    const sub = this.buscar$.pipe(
      debounceTime(250),
      switchMap(q => this.admin.productos({q, categoria: null, disponible: null, page: 0}, 8))
    ).subscribe({
      next: pg => {
        this.resultados.set(pg.content);
        this.buscando.set(false);
      },
      error: () => this.buscando.set(false)
    });
    inject(DestroyRef).onDestroy(() => sub.unsubscribe());
  }

  ngOnInit(): void {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.carro(this.id).subscribe({
      next: c => {
        this.carro.set(c);
        if (this.route.snapshot.queryParamMap.get('cargar') && c.activo) this.abrirCarga();
      },
      error: err => this.error.set(apiMessage(err, 'No encontramos este carro.'))
    });
    this.cargarRutas();
  }

  clave(productoId: number, presentacion: string | null): string {
    return productoId + '|' + (presentacion ?? '').toLowerCase();
  }

  fraccion(s: StockItem): boolean {
    return !s.presentacion && FRACCIONABLES.includes(s.unidadVenta);
  }

  unidad(s: StockItem): string {
    if (s.presentacion) return s.cantidad === 1 ? 'unidad' : 'unidades';
    const nombres: Record<UnidadVenta, [string, string]> = {
      UNIDAD: ['unidad', 'unidades'], LIBRA: ['libra', 'libras'], KILO: ['kilo', 'kilos'],
      GRAMOS: ['gramos', 'gramos'], PAQUETE: ['paquete', 'paquetes'], CAJA: ['caja', 'cajas']
    };
    return nombres[s.unidadVenta][s.cantidad === 1 ? 0 : 1];
  }

  /** La plata de los pedidos de la ruta (sin los anulados). */
  vendidoEn(r: RutaCarro): number {
    return r.pedidos.filter(p => p.estado !== 'ANULADO').reduce((t, p) => t + p.total, 0);
  }

  abrevItem(i: RutaItem): string {
    return abrevUnidad(i.unidadVenta, i.presentacion);
  }

  /** «hoy», «ayer» o «lunes 9 de noviembre», para la frase de la ruta en curso. */
  diaMin(iso: string): string {
    const d = dia(iso);
    return d === 'Hoy' || d === 'Ayer' ? d.toLowerCase() : 'el ' + d.toLowerCase();
  }

  /** Vuelve a traer las rutas desde la más reciente (después de cargar, vender o quitar). */
  cargarRutas(): void {
    this.paginaRutas = 0;
    this.api.rutas(this.id, 0, RUTAS_POR_PAGINA).subscribe({
      next: pg => {
        this.rutas.set(pg.content);
        this.hayMasRutas.set(pg.totalPages > 1);
      },
      error: () => undefined
    });
  }

  masRutas(): void {
    this.cargandoRutas.set(true);
    this.api.rutas(this.id, this.paginaRutas + 1, RUTAS_POR_PAGINA).subscribe({
      next: pg => {
        this.paginaRutas = pg.page;
        this.rutas.update(rs => [...rs, ...pg.content]);
        this.hayMasRutas.set(pg.page < pg.totalPages - 1);
        this.cargandoRutas.set(false);
      },
      error: () => this.cargandoRutas.set(false)
    });
  }

  // ---- Ruta nueva ----

  empezarRuta(llevarSobrante: boolean): void {
    this.ocupado.set(true);
    this.errorRuta.set('');
    this.api.nuevaRuta(this.id, llevarSobrante).subscribe({
      next: c => {
        this.carro.set(c);
        this.ocupado.set(false);
        this.nuevaRutaAbierta.set(false);
        this.cargarRutas();
        // Lo normal al empezar una ruta es subirle mercancía: se abre la carga de una vez.
        this.abrirCarga();
        this.aviso.set(llevarSobrante ? 'Empezaste una ruta nueva. Lo que sobró sigue en el carro.'
          : 'Empezaste una ruta nueva. Lo que sobró se bajó del carro.');
      },
      error: err => {
        this.ocupado.set(false);
        this.errorRuta.set(apiMessage(err));
      }
    });
  }

  // ---- Carga ----

  abrirCarga(): void {
    this.lineas.set([]);
    this.busqueda.set('');
    this.resultados.set([]);
    this.nota = '';
    this.errorCarga.set('');
    this.cargaAbierta.set(true);
    // Quita ?cargar=1 para que recargar la página no vuelva a abrir el formulario.
    void this.router.navigate([], {queryParams: {}, replaceUrl: true});
  }

  cerrarCarga(): void {
    this.cargaAbierta.set(false);
  }

  buscar(q: string): void {
    this.busqueda.set(q);
    if (!q.trim()) {
      this.resultados.set([]);
      return;
    }
    this.buscando.set(true);
    this.buscar$.next(q.trim());
  }

  yaEsta(o: Opcion): boolean {
    return this.lineas().some(l => l.clave === this.clave(o.producto.id, o.presentacion));
  }

  agregar(o: Opcion): void {
    const clave = this.clave(o.producto.id, o.presentacion);
    const existente = this.lineas().find(l => l.clave === clave);
    if (existente) {
      const n = leerCantidad(existente.texto) ?? 0;
      this.cambiarTexto(existente, String(n + 1).replace('.', ','));
      return;
    }
    this.lineas.update(ls => [...ls, {
      clave, productoId: o.producto.id, nombre: o.producto.nombre, presentacion: o.presentacion,
      etiqueta: o.presentacion ?? o.producto.presentacion,
      fraccionable: !o.presentacion && FRACCIONABLES.includes(o.producto.unidadVenta), texto: '1'
    }]);
  }

  cambiarTexto(l: LineaCarga, texto: string): void {
    this.lineas.update(ls => ls.map(x => x.clave === l.clave ? {...x, texto} : x));
  }

  quitar(l: LineaCarga): void {
    this.lineas.update(ls => ls.filter(x => x.clave !== l.clave));
  }

  valida(l: LineaCarga): boolean {
    const n = leerCantidad(l.texto);
    return n !== null && n > 0 && (l.fraccionable || Number.isInteger(n));
  }

  todasValidas(): boolean {
    return this.lineas().every(l => this.valida(l));
  }

  confirmarCarga(): void {
    if (!this.todasValidas()) {
      this.errorCarga.set('Revisa las cantidades marcadas: deben ser mayores que cero, y enteras salvo en libras y kilos.');
      return;
    }
    const items = this.lineas().map(l => ({productoId: l.productoId, presentacion: l.presentacion, cantidad: leerCantidad(l.texto)!}));
    this.ocupado.set(true);
    this.errorCarga.set('');
    this.api.cargar(this.id, items, this.nota).subscribe({
      next: c => {
        this.carro.set(c);
        this.ocupado.set(false);
        this.cargaAbierta.set(false);
        this.aviso.set(items.length === 1 ? 'Agregaste 1 producto al carro.' : `Agregaste ${items.length} productos al carro.`);
        this.cargarRutas();
      },
      error: err => {
        this.ocupado.set(false);
        this.errorCarga.set(apiMessage(err));
      }
    });
  }

  // ---- Sumar, restar o quitar un producto ----

  editarFila(s: StockItem): void {
    this.fila.set(this.clave(s.productoId, s.presentacion));
    this.textoFila.set(this.texto(s.cantidad));
    this.errorFila.set('');
    this.aviso.set('');
  }

  sumarFila(delta: number): void {
    const n = leerCantidad(this.textoFila()) ?? 0;
    this.textoFila.set(this.texto(Math.max(0, Math.round((n + delta) * 1000) / 1000)));
  }

  guardarFila(s: StockItem): void {
    const n = leerCantidad(this.textoFila());
    if (n === null) { this.errorFila.set('Escribe una cantidad.'); return; }
    if (!this.fraccion(s) && !Number.isInteger(n)) { this.errorFila.set('Este producto va en cantidades enteras.'); return; }
    if (n === s.cantidad) { this.fila.set(null); return; }
    this.cambiarCantidad(s, n);
  }

  quitarFila(s: StockItem): void {
    this.cambiarCantidad(s, 0);
  }

  private cambiarCantidad(s: StockItem, n: number): void {
    this.ocupado.set(true);
    this.errorFila.set('');
    const antes = s.cantidad;
    this.api.ajustar(this.id, [{productoId: s.productoId, presentacion: s.presentacion, cantidad: n}], '').subscribe({
      next: c => {
        this.carro.set(c);
        this.ocupado.set(false);
        this.fila.set(null);
        this.aviso.set(n === 0 ? `Quitaste «${s.nombre}» del carro.`
          : n < antes ? `Le restaste ${cantidad(antes - n)} a «${s.nombre}». Quedan ${cantidad(n)}.`
          : `Le sumaste ${cantidad(n - antes)} a «${s.nombre}». Quedan ${cantidad(n)}.`);
        this.cargarRutas();
      },
      error: err => {
        this.ocupado.set(false);
        this.errorFila.set(apiMessage(err));
      }
    });
  }

  /** 2,5 para escribir en el campo (sin separador de miles). */
  private texto(n: number): string {
    return cantidad(n).replace(/\./g, '');
  }

  // ---- Descargue ----

  descargar(): void {
    const c = this.carro();
    if (!c || !confirm(`¿Vaciar «${c.nombre}»? Todo lo que lleva vuelve al local.`)) return;
    this.ocupado.set(true);
    this.error.set('');
    this.api.descargar(this.id, '').subscribe({
      next: act => {
        this.carro.set(act);
        this.ocupado.set(false);
        this.aviso.set('Carro vaciado.');
        this.cargarRutas();
      },
      error: err => {
        this.ocupado.set(false);
        this.error.set(apiMessage(err));
      }
    });
  }

  // ---- Editar ----

  abrirEditar(): void {
    const c = this.carro();
    if (!c) return;
    this.errorEditar.set('');
    this.editando.set({nombre: c.nombre, placa: c.placa ?? '', activo: c.activo});
  }

  guardarCarro(f: CarroForm): void {
    this.ocupado.set(true);
    this.api.actualizarCarro(this.id, f).subscribe({
      next: c => {
        this.carro.set(c);
        this.ocupado.set(false);
        this.editando.set(null);
        this.aviso.set('Carro guardado.');
      },
      error: err => {
        this.ocupado.set(false);
        this.errorEditar.set(apiMessage(err));
      }
    });
  }

  eliminar(): void {
    const c = this.carro();
    if (!c || !confirm(`¿Eliminar «${c.nombre}»? Los pedidos que hiciste desde él se conservan.`)) return;
    this.ocupado.set(true);
    this.api.eliminarCarro(this.id).subscribe({
      next: () => void this.router.navigate(['/admin/carros'], {queryParams: {eliminado: c.nombre}}),
      error: err => {
        this.ocupado.set(false);
        // Desde «Editar carro» el error va en la ventana; desde la página, arriba.
        if (this.editando()) this.errorEditar.set(apiMessage(err)); else this.error.set(apiMessage(err));
      }
    });
  }
}
