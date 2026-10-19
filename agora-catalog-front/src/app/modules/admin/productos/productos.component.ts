import {Component, computed, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {debounceTime, Subject} from 'rxjs';

import {apiMessage, AuthService} from '@app/core/services/auth.service';
import {PanelShellComponent} from '@app/shared/layouts/panel-shell/panel-shell.component';
import {ADMIN_LINKS, AdminService, Categoria, cop, FiltroProductos, Pagina, Producto} from '../admin.service';

const POR_PAGINA = 20;

/** Listado de productos: buscar, filtrar, encender o apagar disponible y editar precios rápido. */
@Component({
  selector: 'app-productos',
  imports: [PanelShellComponent, RouterLink],
  template: `
    <app-panel-shell [subtitle]="empresa()" [links]="links">
      <header class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 class="page-title">Productos</h1>
          <p class="mt-1 text-muted">{{ resumen() }}</p>
        </div>
        <a class="btn btn-dark" routerLink="/admin/productos/nuevo">+ Nuevo producto</a>
      </header>

      @if (error()) { <p class="alert-error mt-6" role="alert">{{ error() }}</p> }
      @if (aviso()) { <p class="alert-ok mt-6" role="status">{{ aviso() }}</p> }

      <div class="mt-6 grid gap-3 sm:grid-cols-[1fr_220px_220px]">
        <input class="input" type="search" placeholder="Buscar por nombre o código" aria-label="Buscar productos"
               [value]="filtro().q" (input)="buscar$.next($any($event.target).value)">
        <select class="input" aria-label="Categoría" (change)="filtrar({categoria: +$any($event.target).value || null})">
          <option value="" [selected]="!filtro().categoria">Todas las categorías</option>
          @for (c of categorias(); track c.id) { <option [value]="c.id" [selected]="filtro().categoria === c.id">{{ c.nombre }}</option> }
        </select>
        <select class="input" aria-label="Disponibilidad" (change)="filtrarDisponible($any($event.target).value)">
          <option value="" [selected]="filtro().disponible === null">Disponibles y agotados</option>
          <option value="true" [selected]="filtro().disponible === true">Solo disponibles</option>
          <option value="false" [selected]="filtro().disponible === false">Solo agotados</option>
        </select>
      </div>

      <div class="card mt-4 overflow-x-auto">
        <table class="table min-w-[760px]">
          <thead><tr>
            <th><span class="sr-only">Foto</span></th><th>Código</th><th>Producto</th><th>Categoría</th>
            <th class="text-right">Precio</th><th>Oferta</th><th>Disponible</th><th><span class="sr-only">Editar</span></th>
          </tr></thead>
          <tbody>
            @for (p of pagina()?.content ?? []; track p.id) {
              <tr>
                <td>
                  @if (p.imagenThumbUrl) {
                    <img [src]="p.imagenThumbUrl" alt="" class="size-11 rounded-lg object-cover" loading="lazy">
                  } @else {
                    <span class="grid size-11 place-items-center rounded-lg bg-wash text-xs text-faint" aria-label="Sin foto">—</span>
                  }
                </td>
                <td class="font-mono text-xs text-muted">{{ p.codigo }}</td>
                <td><b>{{ p.nombre }}</b>@if (p.presentacion) {<div class="text-xs text-muted">{{ p.presentacion }}</div>}</td>
                <td>{{ p.categoriaNombre }}</td>
                <td class="text-right">
                  <input class="input w-28 text-right tabular-nums" inputmode="numeric" [attr.aria-label]="'Precio de ' + p.nombre"
                         [class.border-ink]="cambios().has(p.id)" [value]="miles(cambios().get(p.id) ?? p.precio)"
                         (input)="cambiarPrecio(p, $any($event.target))">
                </td>
                <td>
                  @if (p.precioOferta !== null) {
                    <span class="badge" [class]="p.ofertaVigente ? 'bg-[#FFD43B] text-[#3A2C00]' : 'bg-wash text-muted'">{{ cop(p.precioOferta) }}</span>
                  } @else { <span class="text-muted">No</span> }
                </td>
                <td>
                  <input type="checkbox" class="switch" role="switch" [checked]="p.disponible"
                         [attr.aria-label]="p.nombre + ' disponible'" (change)="disponible(p, $event)">
                </td>
                <td><a class="btn min-h-9 px-3 text-sm" [routerLink]="['/admin/productos', p.id]" [attr.aria-label]="'Editar ' + p.nombre">Editar</a></td>
              </tr>
            } @empty {
              <tr><td colspan="8" class="py-10 text-center text-muted">
                {{ cargando() ? 'Cargando…' : hayFiltros() ? 'Ningún producto con estos filtros.' : 'Todavía no tienes productos. Crea el primero.' }}
              </td></tr>
            }
          </tbody>
        </table>
      </div>

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

      @if (cambios().size) {
        <div class="sticky bottom-4 mt-6 flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-ink px-5 py-3 text-white shadow-lg" role="region" aria-label="Precios sin guardar">
          <span>{{ cambios().size === 1 ? '1 precio sin guardar' : cambios().size + ' precios sin guardar' }}</span>
          <div class="flex gap-2">
            <button type="button" class="btn min-h-10 border-[#4A4038] bg-transparent text-white hover:bg-white/10" (click)="descartar()">Descartar</button>
            <button type="button" class="btn min-h-10" [disabled]="guardando()" (click)="guardarPrecios()">{{ guardando() ? 'Guardando…' : 'Guardar precios' }}</button>
          </div>
        </div>
      }
    </app-panel-shell>
  `
})
export class ProductosComponent implements OnInit {

  private readonly api = inject(AdminService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly links = ADMIN_LINKS;
  readonly cop = cop;

  readonly empresa = computed(() => this.auth.usuario()?.empresaNombre ?? 'Panel del negocio');
  readonly categorias = signal<Categoria[]>([]);
  readonly pagina = signal<Pagina<Producto> | null>(null);
  readonly filtro = signal<FiltroProductos>({q: '', categoria: null, disponible: null, page: 0});
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly aviso = signal('');
  readonly guardando = signal(false);
  /** Precios editados en la tabla y no guardados todavía (id -> precio). */
  readonly cambios = signal<Map<number, number>>(new Map());
  readonly buscar$ = new Subject<string>();

  readonly hayFiltros = computed(() => !!(this.filtro().q || this.filtro().categoria || this.filtro().disponible !== null));
  readonly resumen = computed(() => {
    const n = this.pagina()?.totalElements;
    return n === undefined ? '' : this.hayFiltros() ? `${n} con estos filtros.` : `${n} ${n === 1 ? 'producto' : 'productos'} en tu catálogo.`;
  });

  constructor() {
    const sub = this.buscar$.pipe(debounceTime(300)).subscribe(q => this.filtrar({q}));
    inject(DestroyRef).onDestroy(() => sub.unsubscribe());
  }

  ngOnInit(): void {
    const q = this.route.snapshot.queryParamMap;
    this.filtro.set({
      q: q.get('q') ?? '', categoria: Number(q.get('categoria')) || null,
      disponible: q.get('disponible') === 'true' ? true : q.get('disponible') === 'false' ? false : null,
      page: Math.max(0, Number(q.get('page')) || 0)
    });
    this.api.categorias().subscribe({next: c => this.categorias.set(c), error: () => undefined});
    this.cargar();
  }

  filtrar(cambio: Partial<FiltroProductos>): void {
    this.filtro.update(f => ({...f, ...cambio, page: 0}));
    this.cargar();
  }

  filtrarDisponible(valor: string): void {
    this.filtrar({disponible: valor === '' ? null : valor === 'true'});
  }

  irA(page: number): void {
    this.filtro.update(f => ({...f, page}));
    this.cargar();
  }

  miles(n: number): string {
    return cop(n).slice(1);
  }

  cambiarPrecio(p: Producto, input: HTMLInputElement): void {
    const digitos = input.value.replace(/\D/g, '');
    const precio = digitos ? Math.min(Number(digitos), 999_999_999) : 0;
    const next = new Map(this.cambios());
    if (digitos && precio !== p.precio) next.set(p.id, precio); else next.delete(p.id);
    this.cambios.set(next);
  }

  descartar(): void {
    this.cambios.set(new Map());
    this.cargar();
  }

  guardarPrecios(): void {
    this.guardando.set(true);
    this.error.set('');
    const cambios = [...this.cambios()].map(([id, precio]) => ({id, precio}));
    this.api.cambiarPrecios(cambios).subscribe({
      next: () => {
        this.aviso.set(cambios.length === 1 ? 'Precio guardado.' : `${cambios.length} precios guardados.`);
        this.cambios.set(new Map());
        this.guardando.set(false);
        this.cargar();
      },
      error: err => {
        this.error.set(apiMessage(err));
        this.guardando.set(false);
      }
    });
  }

  disponible(p: Producto, e: Event): void {
    const input = e.target as HTMLInputElement, valor = input.checked;
    this.api.setDisponible(p.id, valor).subscribe({
      next: actualizado => this.pagina.update(pg => pg && {...pg, content: pg.content.map(x => x.id === p.id ? actualizado : x)}),
      error: err => {
        input.checked = !valor;
        this.error.set(apiMessage(err));
      }
    });
  }

  private cargar(): void {
    const f = this.filtro();
    // Los filtros quedan en la URL: volver del formulario deja la lista donde estaba.
    void this.router.navigate([], {
      queryParams: {q: f.q || null, categoria: f.categoria, disponible: f.disponible, page: f.page || null},
      replaceUrl: true
    });
    this.cargando.set(true);
    this.api.productos(f, POR_PAGINA).subscribe({
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
