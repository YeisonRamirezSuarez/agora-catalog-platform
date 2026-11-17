import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';

import {apiMessage, AuthService} from '@app/core/services/auth.service';
import {PanelShellComponent} from '@app/shared/layouts/panel-shell/panel-shell.component';
import {ADMIN_LINKS, cop} from '../admin.service';
import {Carro, RutaService} from './ruta.service';

/** Carros de la venta en ruta: qué lleva cada uno y cuánto vale, y crear uno nuevo. */
@Component({
  selector: 'app-carros',
  imports: [PanelShellComponent, FormsModule, RouterLink],
  template: `
    <app-panel-shell [subtitle]="empresa()" [links]="links">
      <header>
        <h1 class="page-title">Carros</h1>
        <p class="mt-1 max-w-2xl text-muted">
          Lo que sale a la ruta. Carga cada carro con los productos que lleva y el stock se va descontando con cada pedido a una tienda.
        </p>
      </header>

      @if (error()) { <p class="alert-error mt-6" role="alert">{{ error() }}</p> }
      @if (eliminado()) { <p class="alert-ok mt-6" role="status">«{{ eliminado() }}» eliminado. Sus pedidos siguen en el historial.</p> }

      <form class="mt-6 grid gap-2 sm:flex" (ngSubmit)="crear()">
        <input class="input" name="nombre" [(ngModel)]="nombre" placeholder="Nombre del carro nuevo (Carro 1, Moto…)" aria-label="Nombre del carro nuevo" maxlength="60">
        <input class="input sm:w-40" name="placa" [(ngModel)]="placa" placeholder="Placa (opcional)" aria-label="Placa" maxlength="15">
        <button class="btn btn-dark" [disabled]="!nombre.trim() || creando()">{{ creando() ? 'Creando…' : 'Agregar' }}</button>
      </form>

      <ul class="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        @for (c of carros(); track c.id) {
          <li>
            <a class="card grid gap-3 p-4 text-ink no-underline hover:border-line-strong" [class.opacity-60]="!c.activo" [routerLink]="['/admin/carros', c.id]">
              <div class="flex items-start justify-between gap-2">
                <div class="min-w-0">
                  <p class="text-lg font-semibold">{{ c.nombre }}</p>
                  @if (c.placa) { <p class="font-mono text-xs text-muted">{{ c.placa }}</p> }
                </div>
                @if (!c.activo) { <span class="badge bg-wash text-muted">Inactivo</span> }
                @else if (!c.productos) { <span class="badge bg-wash text-muted">Vacío</span> }
              </div>
              <div class="flex items-end justify-between gap-2">
                <span class="text-sm text-muted">{{ c.productos === 1 ? '1 producto' : c.productos + ' productos' }}</span>
                <span class="text-right">
                  <span class="block text-xs text-muted">Valor a precio de catálogo</span>
                  <span class="text-lg font-semibold tabular-nums">{{ cop(c.valor) }}</span>
                </span>
              </div>
            </a>
          </li>
        } @empty {
          <li class="card py-10 text-center text-muted sm:col-span-2 lg:col-span-3">
            {{ cargando() ? 'Cargando…' : 'Todavía no tienes carros. Agrega el primero arriba.' }}
          </li>
        }
      </ul>
    </app-panel-shell>
  `
})
export class CarrosComponent implements OnInit {

  private readonly api = inject(RutaService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly links = ADMIN_LINKS;
  readonly cop = cop;

  readonly empresa = computed(() => this.auth.usuario()?.empresaNombre ?? 'Panel del negocio');
  readonly carros = signal<Carro[]>([]);
  readonly cargando = signal(true);
  readonly creando = signal(false);
  readonly error = signal('');
  readonly eliminado = signal(inject(ActivatedRoute).snapshot.queryParamMap.get('eliminado') ?? '');
  nombre = '';
  placa = '';

  ngOnInit(): void {
    this.api.carros().subscribe({
      next: c => {
        this.carros.set(c);
        this.cargando.set(false);
      },
      error: err => {
        this.error.set(apiMessage(err));
        this.cargando.set(false);
      }
    });
  }

  crear(): void {
    this.creando.set(true);
    this.error.set('');
    this.api.crearCarro({nombre: this.nombre, placa: this.placa, activo: true}).subscribe({
      // Recién creado está vacío: lo siguiente es cargarlo.
      next: c => void this.router.navigate(['/admin/carros', c.id], {queryParams: {cargar: 1}}),
      error: err => {
        this.creando.set(false);
        this.error.set(apiMessage(err));
      }
    });
  }
}
