import {Component, computed, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {debounceTime, Subject} from 'rxjs';

import {apiMessage, AuthService} from '@app/core/services/auth.service';
import {PanelShellComponent} from '@app/shared/layouts/panel-shell/panel-shell.component';
import {ADMIN_LINKS, Pagina} from '../admin.service';
import {FiltroTiendas, RutaService, Tienda, TiendaForm} from './ruta.service';

const POR_PAGINA = 20;
const VACIA: TiendaForm = {nombre: '', encargado: '', telefono: '', direccion: '', barrio: '', notas: '', activa: true};

/** Tiendas de la venta en ruta: buscar, crear, editar y desactivar. */
@Component({
  selector: 'app-tiendas',
  imports: [PanelShellComponent, FormsModule],
  template: `
    <app-panel-shell [subtitle]="empresa()" [links]="links">
      <header class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 class="page-title">Tiendas</h1>
          <p class="mt-1 text-muted">{{ resumen() }}</p>
        </div>
        <button type="button" class="btn btn-dark" (click)="abrir(null)">+ Nueva tienda</button>
      </header>

      @if (error()) { <p class="alert-error mt-6" role="alert">{{ error() }}</p> }
      @if (aviso()) { <p class="alert-ok mt-6" role="status">{{ aviso() }}</p> }

      <div class="mt-6 grid gap-3 sm:grid-cols-[1fr_220px]">
        <input class="input" type="search" placeholder="Buscar por nombre, encargado o barrio" aria-label="Buscar tiendas"
               [value]="filtro().q" (input)="buscar$.next($any($event.target).value)">
        <select class="input" aria-label="Estado" (change)="filtrarActiva($any($event.target).value)">
          <option value="" [selected]="filtro().activa === null">Activas e inactivas</option>
          <option value="true" [selected]="filtro().activa === true">Solo activas</option>
          <option value="false" [selected]="filtro().activa === false">Solo inactivas</option>
        </select>
      </div>

      <ul class="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        @for (t of pagina()?.content ?? []; track t.id) {
          <li class="card grid gap-2 p-4" [class.opacity-60]="!t.activa">
            <div class="flex items-start justify-between gap-2">
              <div class="min-w-0">
                <p class="font-semibold">{{ t.nombre }}</p>
                @if (t.encargado) { <p class="truncate text-sm text-muted">{{ t.encargado }}</p> }
              </div>
              <input type="checkbox" class="switch shrink-0" role="switch" [checked]="t.activa"
                     [attr.aria-label]="t.nombre + ' activa'" (change)="activa(t, $event)">
            </div>
            @if (t.direccion || t.barrio) {
              <p class="text-sm text-ink-soft">{{ t.direccion }}@if (t.direccion && t.barrio) { · }{{ t.barrio }}</p>
            }
            @if (t.telefono) {
              <a class="justify-self-start text-sm font-semibold" [href]="'tel:' + t.telefono">{{ t.telefono }}</a>
            }
            <div class="mt-1 flex flex-wrap items-center gap-2">
              <span class="text-xs text-muted">{{ t.pedidos === 1 ? '1 pedido' : t.pedidos + ' pedidos' }}</span>
              <span class="flex-1"></span>
              <button type="button" class="btn min-h-9 px-3 text-sm" (click)="abrir(t)" [attr.aria-label]="'Editar ' + t.nombre">Editar</button>
              @if (!t.pedidos) {
                <button type="button" class="btn min-h-9 px-3 text-sm text-danger" (click)="eliminar(t)" [attr.aria-label]="'Eliminar ' + t.nombre">Eliminar</button>
              }
            </div>
          </li>
        } @empty {
          <li class="card py-10 text-center text-muted sm:col-span-2 lg:col-span-3">
            {{ cargando() ? 'Cargando…' : hayFiltros() ? 'Ninguna tienda con estos filtros.' : 'Todavía no tienes tiendas. Agrega las que visitas en la ruta.' }}
          </li>
        }
      </ul>

      @if (pagina(); as pg) {
        @if (pg.totalPages > 1) {
          <nav class="mt-4 flex items-center justify-between gap-3 text-sm" aria-label="Paginación">
            <span class="text-muted">{{ pg.page * pg.size + 1 }} a {{ pg.page * pg.size + pg.content.length }} de {{ pg.totalElements }}</span>
            <div class="flex gap-2">
              <button type="button" class="btn" [disabled]="pg.page === 0" (click)="irA(pg.page - 1)">Anterior</button>
              <button type="button" class="btn" [disabled]="pg.page >= pg.totalPages - 1" (click)="irA(pg.page + 1)">Siguiente</button>
            </div>
          </nav>
        }
      }

      @if (editando()) {
        <div class="fixed inset-0 z-50 grid place-items-end bg-black/50 sm:place-items-center sm:p-4" (click)="$event.target === $event.currentTarget ? cerrar() : null">
          <form class="card grid max-h-dvh w-full max-w-lg gap-4 overflow-y-auto rounded-b-none p-5 sm:rounded-b-2xl sm:p-6" role="dialog" aria-modal="true"
                [attr.aria-label]="tiendaId() ? 'Editar tienda' : 'Nueva tienda'" (ngSubmit)="guardar()">
            <h2 class="text-xl font-semibold">{{ tiendaId() ? 'Editar tienda' : 'Nueva tienda' }}</h2>
            @if (errorForm()) { <p class="alert-error" role="alert">{{ errorForm() }}</p> }
            <div class="field"><label for="t-nom">Nombre de la tienda</label>
              <input class="input" id="t-nom" name="nombre" maxlength="80" required [(ngModel)]="form.nombre" placeholder="Tienda Doña Rosa"></div>
            <div class="grid gap-4 sm:grid-cols-2">
              <div class="field"><label for="t-enc">Encargado</label>
                <input class="input" id="t-enc" name="encargado" maxlength="80" [(ngModel)]="form.encargado"></div>
              <div class="field"><label for="t-tel">Teléfono o WhatsApp</label>
                <input class="input" id="t-tel" name="telefono" type="tel" inputmode="tel" maxlength="20" [(ngModel)]="form.telefono" placeholder="300 123 4567"></div>
            </div>
            <div class="field"><label for="t-dir">Dirección</label>
              <input class="input" id="t-dir" name="direccion" maxlength="150" [(ngModel)]="form.direccion"></div>
            <div class="field"><label for="t-bar">Barrio</label>
              <input class="input" id="t-bar" name="barrio" maxlength="80" [(ngModel)]="form.barrio"></div>
            <div class="field"><label for="t-not">Notas</label>
              <textarea class="input py-2" id="t-not" name="notas" rows="2" maxlength="300" [(ngModel)]="form.notas" placeholder="Abre a las 7, pedir por la puerta de atrás…"></textarea></div>
            <label class="inline-flex items-center gap-2 font-semibold">
              <input type="checkbox" class="switch" role="switch" name="activa" [(ngModel)]="form.activa">Activa
            </label>
            <div class="flex flex-wrap gap-2">
              <button class="btn btn-dark" [disabled]="guardando() || !form.nombre.trim()">{{ guardando() ? 'Guardando…' : 'Guardar' }}</button>
              <button type="button" class="btn-link" (click)="cerrar()">Cancelar</button>
            </div>
          </form>
        </div>
      }
    </app-panel-shell>
  `
})
export class TiendasComponent implements OnInit {

  private readonly api = inject(RutaService);
  private readonly auth = inject(AuthService);
  readonly links = ADMIN_LINKS;

  readonly empresa = computed(() => this.auth.usuario()?.empresaNombre ?? 'Panel del negocio');
  readonly pagina = signal<Pagina<Tienda> | null>(null);
  readonly filtro = signal<FiltroTiendas>({q: '', activa: null, page: 0});
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly aviso = signal('');
  readonly editando = signal(false);
  readonly tiendaId = signal<number | null>(null);
  readonly guardando = signal(false);
  readonly errorForm = signal('');
  readonly buscar$ = new Subject<string>();
  form: TiendaForm = {...VACIA};

  readonly hayFiltros = computed(() => !!(this.filtro().q || this.filtro().activa !== null));
  readonly resumen = computed(() => {
    const n = this.pagina()?.totalElements;
    return n === undefined ? '' : this.hayFiltros() ? `${n} con estos filtros.`
      : `${n} ${n === 1 ? 'tienda' : 'tiendas'} en tu ruta.`;
  });

  constructor() {
    const sub = this.buscar$.pipe(debounceTime(300)).subscribe(q => {
      this.filtro.update(f => ({...f, q, page: 0}));
      this.cargar();
    });
    inject(DestroyRef).onDestroy(() => sub.unsubscribe());
  }

  ngOnInit(): void {
    this.cargar();
  }

  filtrarActiva(valor: string): void {
    this.filtro.update(f => ({...f, activa: valor === '' ? null : valor === 'true', page: 0}));
    this.cargar();
  }

  irA(page: number): void {
    this.filtro.update(f => ({...f, page}));
    this.cargar();
  }

  abrir(t: Tienda | null): void {
    this.tiendaId.set(t?.id ?? null);
    this.form = t ? {
      nombre: t.nombre, encargado: t.encargado ?? '', telefono: t.telefono ?? '', direccion: t.direccion ?? '',
      barrio: t.barrio ?? '', notas: t.notas ?? '', activa: t.activa
    } : {...VACIA};
    this.errorForm.set('');
    this.editando.set(true);
  }

  cerrar(): void {
    this.editando.set(false);
  }

  guardar(): void {
    this.guardando.set(true);
    this.errorForm.set('');
    const id = this.tiendaId();
    (id ? this.api.actualizarTienda(id, this.form) : this.api.crearTienda(this.form)).subscribe({
      next: t => {
        this.guardando.set(false);
        this.editando.set(false);
        this.aviso.set(id ? `«${t.nombre}» guardada.` : `«${t.nombre}» agregada.`);
        this.cargar();
      },
      error: err => {
        this.guardando.set(false);
        this.errorForm.set(apiMessage(err));
      }
    });
  }

  activa(t: Tienda, e: Event): void {
    const input = e.target as HTMLInputElement, valor = input.checked;
    this.api.setTiendaActiva(t.id, valor).subscribe({
      next: act => this.pagina.update(pg => pg && {...pg, content: pg.content.map(x => x.id === t.id ? act : x)}),
      error: err => {
        input.checked = !valor;
        this.error.set(apiMessage(err));
      }
    });
  }

  eliminar(t: Tienda): void {
    if (!confirm(`¿Eliminar la tienda «${t.nombre}»?`)) return;
    this.api.eliminarTienda(t.id).subscribe({
      next: () => {
        this.aviso.set(`«${t.nombre}» eliminada.`);
        this.cargar();
      },
      error: err => this.error.set(apiMessage(err))
    });
  }

  private cargar(): void {
    this.cargando.set(true);
    this.api.tiendas(this.filtro(), POR_PAGINA).subscribe({
      next: pg => {
        this.pagina.set(pg);
        this.cargando.set(false);
      },
      error: err => {
        this.error.set(apiMessage(err));
        this.cargando.set(false);
      }
    });
  }
}
