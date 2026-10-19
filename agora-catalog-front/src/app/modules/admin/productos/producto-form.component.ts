import {Component, computed, inject, input, OnInit, signal} from '@angular/core';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router, RouterLink} from '@angular/router';

import {apiMessage, AuthService} from '@app/core/services/auth.service';
import {PanelShellComponent} from '@app/shared/layouts/panel-shell/panel-shell.component';
import {prepararFoto} from '@app/shared/utils/foto';
import {ADMIN_LINKS, AdminService, Categoria, Producto, ProductoForm, UNIDADES, UnidadVenta} from '../admin.service';


/** Crear o editar un producto, con su foto. */
@Component({
  selector: 'app-producto-form',
  imports: [PanelShellComponent, ReactiveFormsModule, RouterLink],
  template: `
    <app-panel-shell [subtitle]="empresa()" [links]="links">
      <header>
        <a routerLink="/admin/productos" class="text-sm font-semibold text-muted no-underline hover:text-ink">← Productos</a>
        <h1 class="page-title mt-1">{{ producto()?.nombre ?? (id() ? 'Producto' : 'Nuevo producto') }}</h1>
      </header>

      @if (error()) { <p class="alert-error mt-6" role="alert">{{ error() }}</p> }
      @if (aviso()) { <p class="alert-ok mt-6" role="status">{{ aviso() }}</p> }

      <form [formGroup]="form" (ngSubmit)="guardar()" novalidate class="mt-6 grid gap-6 lg:grid-cols-[minmax(0,1fr)_320px]">
        <section class="card grid gap-5 p-5 sm:grid-cols-2 sm:p-6">
          <div class="field">
            <label for="p-codigo">Código</label>
            <input class="input" id="p-codigo" formControlName="codigo" autocapitalize="characters" maxlength="40">
            <p class="hint">Único en tu catálogo. Las fotos del ZIP se asignan por este código.</p>
          </div>
          <div class="field">
            <label for="p-cat">Categoría</label>
            <select class="input" id="p-cat" formControlName="categoriaId">
              <option [ngValue]="null" disabled>Elige una categoría</option>
              @for (c of categorias(); track c.id) { <option [ngValue]="c.id">{{ c.nombre }}</option> }
            </select>
            @if (!categorias().length) { <p class="hint">Primero crea una categoría en <a routerLink="/admin/categorias" class="underline">Categorías</a>.</p> }
          </div>
          <div class="field sm:col-span-2">
            <label for="p-nombre">Nombre</label>
            <input class="input" id="p-nombre" formControlName="nombre" maxlength="120">
          </div>
          <div class="field sm:col-span-2">
            <label for="p-desc">Descripción</label>
            <textarea class="input py-2.5" id="p-desc" formControlName="descripcion" rows="3" maxlength="500"></textarea>
            <p class="hint">Se ve al tocar el producto en el catálogo.</p>
          </div>
          <div class="field">
            <label for="p-pres">Presentación</label>
            <input class="input" id="p-pres" formControlName="presentacion" placeholder="Ej.: Tajado x 250 g" maxlength="60">
          </div>
          <div class="field">
            <label for="p-unidad">Se vende por</label>
            <select class="input" id="p-unidad" formControlName="unidadVenta">
              @for (u of unidades; track u.valor) { <option [value]="u.valor">{{ u.texto }}</option> }
            </select>
          </div>
          <div class="field">
            <label for="p-precio">Precio</label>
            <input class="input" id="p-precio" formControlName="precio" inputmode="numeric" type="number" min="0" step="50">
            <p class="hint">En pesos, sin puntos ni decimales.</p>
          </div>
          <fieldset class="grid gap-4 rounded-xl border border-line p-4 sm:col-span-2 sm:grid-cols-3">
            <legend class="px-1 text-sm font-semibold">Oferta (opcional)</legend>
            <div class="field">
              <label for="p-oferta">Precio de oferta</label>
              <input class="input" id="p-oferta" formControlName="precioOferta" inputmode="numeric" type="number" min="0" step="50">
            </div>
            <div class="field">
              <label for="p-desde">Desde</label>
              <input class="input" id="p-desde" type="date" formControlName="ofertaDesde">
            </div>
            <div class="field">
              <label for="p-hasta">Hasta</label>
              <input class="input" id="p-hasta" type="date" formControlName="ofertaHasta">
            </div>
            <p class="hint sm:col-span-3">Sin fechas, la oferta queda activa hasta que borres el precio de oferta.</p>
          </fieldset>
          <div class="flex flex-wrap gap-6 sm:col-span-2">
            <label class="inline-flex items-center gap-3 font-semibold"><input type="checkbox" class="switch" role="switch" formControlName="disponible"> Disponible</label>
            <label class="inline-flex items-center gap-3 font-semibold"><input type="checkbox" class="switch" role="switch" formControlName="destacado"> Marcar como «Más vendido»</label>
          </div>
        </section>

        <section class="card grid content-start gap-4 p-5">
          <h2 class="text-lg font-semibold">Foto</h2>
          @if (vista()) {
            <img [src]="vista()" alt="" class="aspect-[4/3] w-full rounded-xl object-cover">
          } @else {
            <div class="grid aspect-[4/3] w-full place-items-center rounded-xl bg-wash text-sm text-faint">Sin foto</div>
          }
          <label class="btn cursor-pointer">
            {{ vista() ? 'Cambiar foto' : 'Elegir foto' }}
            <input type="file" class="sr-only" accept="image/jpeg,image/png,image/webp" (change)="elegirFoto($event)">
          </label>
          <p class="hint">JPG, PNG o WebP. Hasta 5 MB y 8000 px por lado.</p>
          @if (producto()?.imagenUrl && !archivo()) {
            <button type="button" class="btn-link justify-self-start text-sm" (click)="quitarFoto()">Quitar foto</button>
          }
          <div class="mt-2 flex flex-wrap gap-2">
            <a class="btn" routerLink="/admin/productos">Cancelar</a>
            <button class="btn btn-dark" [disabled]="guardando()">{{ guardando() ? 'Guardando…' : id() ? 'Guardar cambios' : 'Crear producto' }}</button>
          </div>
          @if (id()) {
            <button type="button" class="btn-link mt-2 justify-self-start text-sm text-danger" (click)="eliminar()">Eliminar producto</button>
          }
        </section>
      </form>
    </app-panel-shell>
  `
})
export class ProductoFormComponent implements OnInit {

  private readonly api = inject(AdminService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly links = ADMIN_LINKS;
  readonly unidades = UNIDADES;
  /** Parámetro de la ruta /admin/productos/:id (vacío al crear). */
  readonly id = input<string>();

  readonly empresa = computed(() => this.auth.usuario()?.empresaNombre ?? 'Panel del negocio');
  readonly categorias = signal<Categoria[]>([]);
  readonly producto = signal<Producto | null>(null);
  readonly archivo = signal<File | null>(null);
  readonly previa = signal('');
  readonly vista = computed(() => this.previa() || this.producto()?.imagenUrl || '');
  readonly error = signal('');
  readonly aviso = signal('');
  readonly guardando = signal(false);

  readonly form = inject(FormBuilder).group({
    codigo: ['', [Validators.required, Validators.maxLength(40)]],
    categoriaId: [null as number | null, Validators.required],
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    descripcion: [''],
    presentacion: [''],
    unidadVenta: ['UNIDAD' as UnidadVenta, Validators.required],
    precio: [null as number | null, [Validators.required, Validators.min(0)]],
    precioOferta: [null as number | null, Validators.min(0)],
    ofertaDesde: [null as string | null],
    ofertaHasta: [null as string | null],
    disponible: [true],
    destacado: [false],
  });

  ngOnInit(): void {
    this.api.categorias().subscribe({
      next: c => {
        this.categorias.set(c);
        if (!this.id() && c.length === 1) this.form.patchValue({categoriaId: c[0].id});
      },
      error: err => this.error.set(apiMessage(err))
    });
    const id = Number(this.id());
    if (id) {
      this.api.producto(id).subscribe({
        next: p => {
          this.producto.set(p);
          this.form.patchValue({...p, descripcion: p.descripcion ?? '', presentacion: p.presentacion ?? ''});
        },
        error: err => this.error.set(apiMessage(err))
      });
    }
  }

  async elegirFoto(e: Event): Promise<void> {
    const input = e.target as HTMLInputElement, elegida = input.files?.[0];
    input.value = '';
    if (!elegida) return;
    let f: File;
    try {
      f = await prepararFoto(elegida);
    } catch (err) {
      this.error.set((err as Error).message);
      return;
    }
    this.error.set('');
    this.archivo.set(f);
    this.previa.set(URL.createObjectURL(f));
  }

  guardar(): void {
    this.form.markAllAsTouched();
    this.aviso.set('');
    const v = this.form.getRawValue();
    const faltan = !v.codigo?.trim() || !v.nombre?.trim() || v.categoriaId === null || v.precio === null;
    if (faltan || this.form.invalid) {
      this.error.set('Completa código, categoría, nombre y precio.');
      return;
    }
    const datos: ProductoForm = {
      codigo: v.codigo!.trim(), categoriaId: v.categoriaId, nombre: v.nombre!.trim(), descripcion: v.descripcion ?? '',
      presentacion: v.presentacion ?? '', unidadVenta: v.unidadVenta!, precio: Math.round(v.precio!),
      precioOferta: v.precioOferta === null || (v.precioOferta as unknown) === '' ? null : Math.round(v.precioOferta),
      ofertaDesde: v.ofertaDesde || null, ofertaHasta: v.ofertaHasta || null,
      disponible: !!v.disponible, destacado: !!v.destacado
    };
    this.guardando.set(true);
    this.error.set('');
    const id = Number(this.id());
    const guardar$ = id ? this.api.actualizarProducto(id, datos) : this.api.crearProducto(datos);
    guardar$.subscribe({
      next: p => {
        const foto = this.archivo();
        if (!foto) return this.listo(p, !id);
        // La foto se sube después de guardar: un producto nuevo necesita su id.
        this.api.subirFoto(p.id, foto).subscribe({
          next: conFoto => this.listo(conFoto, !id),
          error: err => {
            this.guardando.set(false);
            this.producto.set(p);
            this.error.set(`El producto quedó guardado, pero la foto no: ${apiMessage(err)}`);
            if (!id) void this.router.navigate(['/admin/productos', p.id], {replaceUrl: true});
          }
        });
      },
      error: err => {
        this.error.set(apiMessage(err));
        this.guardando.set(false);
      }
    });
  }

  quitarFoto(): void {
    const p = this.producto();
    if (!p) return;
    this.api.quitarFoto(p.id).subscribe({
      next: actualizado => this.producto.set(actualizado),
      error: err => this.error.set(apiMessage(err))
    });
  }

  eliminar(): void {
    const p = this.producto();
    if (!p || !confirm(`¿Eliminar «${p.nombre}»? También sale de sus colecciones. No se puede deshacer.`)) return;
    this.api.eliminarProducto(p.id).subscribe({
      next: () => void this.router.navigateByUrl('/admin/productos'),
      error: err => this.error.set(apiMessage(err))
    });
  }

  private listo(p: Producto, nuevo: boolean): void {
    this.guardando.set(false);
    this.archivo.set(null);
    this.previa.set('');
    this.producto.set(p);
    this.aviso.set(nuevo ? 'Producto creado.' : 'Cambios guardados.');
    if (nuevo) void this.router.navigate(['/admin/productos', p.id], {replaceUrl: true});
  }
}
