import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {RouterLink} from '@angular/router';

import {apiMessage, AuthService} from '@app/core/services/auth.service';
import {PanelShellComponent} from '@app/shared/layouts/panel-shell/panel-shell.component';
import {ADMIN_LINKS, cop} from '../admin.service';
import {
  abrevUnidad, cantidad, dia, fechaHora, fechaIso, hora, numeroPedido, ProductoVendido, RutaDia, RutaService, Tablero
} from './ruta.service';

/** Las rutas de un carro en el día. */
interface CarroDelDia {
  carroId: number;
  nombre: string;
  eliminado: boolean;
  vendido: number;
  rutas: RutaDia[];
}

/**
 * Tablero de la venta en ruta, día por día: cuánto se vendió, las rutas de cada carro, lo más
 * vendido y, detrás de un botón, los pedidos del día.
 */
@Component({
  selector: 'app-tablero-rutas',
  imports: [PanelShellComponent, RouterLink],
  template: `
    <app-panel-shell [subtitle]="empresa()" [links]="links">
      <header class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 class="page-title">Rutas</h1>
          <p class="mt-1 text-muted">Cómo le fue a la venta en ruta cada día.</p>
        </div>
        <div class="flex items-center gap-1">
          <button type="button" class="btn min-h-11 min-w-11 px-0" aria-label="Día anterior" (click)="mover(-1)">‹</button>
          <input class="input min-h-11 w-40" type="date" aria-label="Día" [max]="hoy" [value]="fecha()"
                 (change)="irA($any($event.target).value)">
          <button type="button" class="btn min-h-11 min-w-11 px-0" aria-label="Día siguiente" [disabled]="fecha() >= hoy" (click)="mover(1)">›</button>
        </div>
      </header>
      <p class="mt-3 text-sm font-semibold">
        {{ titulo() }}
        @if (fecha() !== hoy) { · <button type="button" class="btn-link text-sm" (click)="irA(hoy)">Ver hoy</button> }
      </p>

      @if (error()) { <p class="alert-error mt-6" role="alert">{{ error() }}</p> }

      @if (tablero(); as t) {
        <div class="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-3">
          <div class="card col-span-2 p-4 sm:col-span-1">
            <p class="text-sm text-muted">Vendido</p>
            <p class="text-3xl font-semibold tabular-nums">{{ cop(t.vendido) }}</p>
          </div>
          <div class="card p-4">
            <p class="text-sm text-muted">Pedidos</p>
            <p class="text-3xl font-semibold tabular-nums">{{ t.pedidos }}</p>
          </div>
          <div class="card p-4">
            <p class="text-sm text-muted">Rutas</p>
            <p class="text-3xl font-semibold tabular-nums">{{ t.rutas.length }}</p>
          </div>
        </div>

        <h2 class="mt-8 text-lg font-semibold">Por carro</h2>
        @for (c of carros(); track c.carroId) {
          <article class="card mt-3 p-4">
            <header class="flex items-baseline justify-between gap-3">
              @if (c.eliminado) {
                <p class="font-semibold">{{ c.nombre }} <span class="text-xs font-normal text-muted">(eliminado)</span></p>
              } @else {
                <a class="font-semibold" [routerLink]="['/admin/carros', c.carroId]">{{ c.nombre }}</a>
              }
              <p class="font-semibold tabular-nums">{{ cop(c.vendido) }}</p>
            </header>
            <ul class="mt-2 divide-y divide-line text-sm">
              @for (r of c.rutas; track r.id) {
                <li class="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1 py-2">
                  <span>
                    <b>Ruta {{ r.numero }}</b>
                    <span class="text-muted"> · {{ salida(r) }}@if (r.cerradaAt) { a {{ hora(r.cerradaAt) }} }</span>
                    @if (!r.cerradaAt) { <span class="badge ml-1 bg-ok-soft text-ok-ink">En curso</span> }
                  </span>
                  <span class="text-muted tabular-nums">{{ r.pedidos === 1 ? '1 pedido' : r.pedidos + ' pedidos' }} · <b class="text-ink">{{ cop(r.vendido) }}</b></span>
                </li>
              }
            </ul>
          </article>
        } @empty {
          <p class="card mt-3 px-4 py-8 text-center text-muted">Ese día no salió ningún carro.</p>
        }

        @if (t.productos.length) {
          <h2 class="mt-8 text-lg font-semibold">Lo más vendido</h2>
          <ul class="card mt-3 divide-y divide-line text-sm">
            @for (p of t.productos; track $index) {
              <li class="grid grid-cols-[1fr_auto_auto] items-baseline gap-3 px-4 py-2">
                <span class="min-w-0 truncate"><b>{{ p.nombre }}</b>@if (p.presentacion) { <span class="text-muted"> · {{ p.presentacion }}</span> }</span>
                <span class="text-muted tabular-nums">{{ cant(p.cantidad) }} {{ abrev(p) }}</span>
                <span class="w-24 text-right font-semibold tabular-nums">{{ cop(p.total) }}</span>
              </li>
            }
          </ul>
        }

        @if (t.pedidosDia.length) {
          <button type="button" class="btn mt-8 w-full" [attr.aria-expanded]="verPedidos()" (click)="verPedidos.set(!verPedidos())">
            {{ verPedidos() ? 'Ocultar pedidos' : 'Ver pedidos (' + t.pedidosDia.length + ')' }}
          </button>
          @if (verPedidos()) {
            <ul class="card mt-3 divide-y divide-line">
              @for (p of t.pedidosDia; track p.id) {
                <li>
                  <a class="grid grid-cols-[1fr_auto] items-center gap-x-3 px-4 py-3 text-sm no-underline hover:bg-wash"
                     [routerLink]="['/admin/pedidos', p.id]">
                    <span class="min-w-0">
                      <span class="block truncate font-semibold">{{ p.tiendaNombre }}</span>
                      <span class="block text-xs text-muted">{{ numero(p.numero) }} · {{ p.carroNombre }} · {{ hora(p.createdAt) }}@if (p.estado === 'ANULADO') { · anulado }</span>
                    </span>
                    <span class="font-semibold tabular-nums" [class.line-through]="p.estado === 'ANULADO'">{{ cop(p.total) }} ›</span>
                  </a>
                </li>
              }
            </ul>
          }
        }
      } @else if (!error()) {
        <p class="mt-6 text-muted">Cargando…</p>
      }
    </app-panel-shell>
  `
})
export class TableroRutasComponent implements OnInit {

  private readonly api = inject(RutaService);
  private readonly auth = inject(AuthService);
  readonly links = ADMIN_LINKS;
  readonly cop = cop;
  readonly cant = cantidad;
  readonly hora = hora;
  readonly numero = numeroPedido;
  readonly hoy = fechaIso(new Date());

  readonly empresa = computed(() => this.auth.usuario()?.empresaNombre ?? 'Panel del negocio');
  readonly fecha = signal(this.hoy);
  readonly tablero = signal<Tablero | null>(null);
  readonly error = signal('');
  readonly verPedidos = signal(false);

  /** «Hoy», «Ayer» o «Lunes 9 de noviembre». */
  readonly titulo = computed(() => dia(this.fecha() + 'T12:00:00'));

  /** Las rutas agrupadas por carro, con lo que vendió cada uno. */
  readonly carros = computed<CarroDelDia[]>(() => {
    const carros = new Map<number, CarroDelDia>();
    for (const r of this.tablero()?.rutas ?? []) {
      const c = carros.get(r.carroId) ?? {carroId: r.carroId, nombre: r.carroNombre, eliminado: r.carroEliminado, vendido: 0, rutas: []};
      c.vendido += r.vendido;
      c.rutas.push(r);
      carros.set(r.carroId, c);
    }
    return [...carros.values()];
  });

  ngOnInit(): void {
    this.cargar();
  }

  irA(fecha: string): void {
    if (!fecha || fecha > this.hoy) return;
    this.fecha.set(fecha);
    this.cargar();
  }

  mover(dias: number): void {
    const d = new Date(this.fecha() + 'T12:00:00');
    d.setDate(d.getDate() + dias);
    this.irA(fechaIso(d));
  }

  /** La hora de salida; con el día si salió otro día (una ruta que sigue en curso desde antes). */
  salida(r: RutaDia): string {
    return fechaIso(new Date(r.iniciadaAt)) === this.fecha() ? hora(r.iniciadaAt) : 'salió el ' + fechaHora(r.iniciadaAt);
  }

  abrev(p: ProductoVendido): string {
    return abrevUnidad(p.unidadVenta, p.presentacion);
  }

  private cargar(): void {
    this.tablero.set(null);
    this.error.set('');
    this.verPedidos.set(false);
    this.api.tablero(this.fecha()).subscribe({
      next: t => this.tablero.set(t),
      error: err => this.error.set(apiMessage(err))
    });
  }
}
