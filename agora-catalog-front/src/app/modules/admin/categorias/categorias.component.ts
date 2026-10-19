import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {CdkDrag, CdkDragDrop, CdkDragHandle, CdkDropList, moveItemInArray} from '@angular/cdk/drag-drop';
import {FormsModule} from '@angular/forms';

import {apiMessage, AuthService} from '@app/core/services/auth.service';
import {PanelShellComponent} from '@app/shared/layouts/panel-shell/panel-shell.component';
import {prepararFoto} from '@app/shared/utils/foto';
import {ADMIN_LINKS, AdminService, Categoria} from '../admin.service';


/** Categorías: agregar, renombrar, ocultar, ordenar arrastrando y elegir la franja grande del inicio. */
@Component({
  selector: 'app-categorias',
  imports: [PanelShellComponent, FormsModule, CdkDropList, CdkDrag, CdkDragHandle],
  template: `
    <app-panel-shell [subtitle]="empresa()" [links]="links">
      <header>
        <h1 class="page-title">Categorías</h1>
        <p class="mt-1 max-w-2xl text-muted">
          Arrastra para cambiar el orden en que aparecen en tu catálogo. Con «Franja», una categoría sale como foto grande en el inicio.
        </p>
      </header>

      @if (error()) { <p class="alert-error mt-6" role="alert">{{ error() }}</p> }
      @if (aviso()) { <p class="alert-ok mt-6" role="status">{{ aviso() }}</p> }

      <form class="mt-6 flex gap-2" (ngSubmit)="agregar()">
        <input class="input" name="nueva" [(ngModel)]="nueva" placeholder="Nombre de la categoría nueva" aria-label="Nombre de la categoría nueva" maxlength="60">
        <button class="btn btn-dark" [disabled]="!nueva.trim()">Agregar</button>
      </form>

      <ol class="card mt-4 divide-y divide-line" cdkDropList (cdkDropListDropped)="soltar($event)">
        @for (c of categorias(); track c.id; let i = $index) {
          <li class="flex flex-wrap items-center gap-3 bg-white px-4 py-3" cdkDrag cdkDragLockAxis="y">
            <button type="button" class="grid size-9 cursor-grab place-items-center rounded-lg text-faint hover:bg-wash" cdkDragHandle
                    [attr.aria-label]="'Mover ' + c.nombre + '. Usa las flechas arriba y abajo.'" (keydown)="teclado($event, i)">⋮⋮</button>
            <div class="min-w-0 flex-1">
              <input class="input h-10 min-h-0 border-transparent bg-transparent px-2 font-semibold hover:border-line-strong focus:bg-white"
                     [value]="c.nombre" maxlength="60" aria-label="Nombre de la categoría" (change)="renombrar(c, $any($event.target))">
              @if (c.bannerActivo) { <span class="badge ml-2 bg-ok-soft text-ok-ink">Franja en el inicio</span> }
            </div>
            <span class="text-sm text-muted">{{ c.productos === 1 ? '1 producto' : c.productos + ' productos' }}</span>
            <label class="inline-flex items-center gap-2 text-sm font-semibold">
              <input type="checkbox" class="switch" role="switch" [checked]="c.activa" [attr.aria-label]="c.nombre + ' visible en el catálogo'"
                     (change)="visible(c, $event)">Visible
            </label>
            <button type="button" class="btn min-h-9 px-3 text-sm" (click)="abrirFranja(c)">Franja</button>
            <button type="button" class="btn min-h-9 px-3 text-sm text-danger" [attr.aria-label]="'Eliminar ' + c.nombre" (click)="eliminar(c)">Eliminar</button>
          </li>
        } @empty {
          <li class="px-4 py-10 text-center text-muted">{{ cargando() ? 'Cargando…' : 'Todavía no tienes categorías. Agrega la primera.' }}</li>
        }
      </ol>

      @if (franja(); as c) {
        <div class="fixed inset-0 z-50 grid place-items-center bg-black/50 p-4" (click)="$event.target === $event.currentTarget ? franja.set(null) : null">
          <form class="card grid w-full max-w-lg gap-4 p-6" role="dialog" aria-modal="true" [attr.aria-label]="'Franja de ' + c.nombre" (ngSubmit)="guardarFranja(c)">
            <h2 class="text-xl font-semibold">Franja de «{{ c.nombre }}» en el inicio</h2>
            <p class="text-sm text-muted">Una foto grande con un título que lleva a esta categoría.
              @if (otraConFranja(c); as otra) { Se la quita a «{{ otra }}»: solo una categoría tiene franja. }</p>
            @if (previaFranja() || c.bannerUrl) {
              <img [src]="previaFranja() || c.bannerUrl" alt="" class="aspect-[8/3] w-full rounded-xl object-cover">
            }
            <label class="btn cursor-pointer justify-self-start">
              {{ previaFranja() || c.bannerUrl ? 'Cambiar foto' : 'Elegir foto' }}
              <input type="file" class="sr-only" accept="image/jpeg,image/png,image/webp" (change)="elegirFoto($event)">
            </label>
            @if (errorFranja()) { <p class="alert-error" role="alert">{{ errorFranja() }}</p> }
            <div class="field"><label for="f-tit">Título</label><textarea class="input py-2" id="f-tit" name="titulo" rows="2" maxlength="40" [(ngModel)]="datos.titulo" placeholder="Combos de la casa"></textarea></div>
            <div class="field"><label for="f-fra">Frase</label><input class="input" id="f-fra" name="frase" maxlength="80" [(ngModel)]="datos.frase"></div>
            <div class="field"><label for="f-not">Nota escrita a mano</label><textarea class="input py-2" id="f-not" name="nota" rows="2" maxlength="32" [(ngModel)]="datos.nota" placeholder="Listos para disfrutar"></textarea></div>
            <div class="flex flex-wrap gap-2">
              <button class="btn btn-dark" [disabled]="guardando()">{{ guardando() ? 'Guardando…' : 'Guardar franja' }}</button>
              @if (c.bannerActivo) { <button type="button" class="btn" (click)="quitarFranja(c)">Quitar franja</button> }
              <button type="button" class="btn-link" (click)="franja.set(null)">Cancelar</button>
            </div>
          </form>
        </div>
      }
    </app-panel-shell>
  `
})
export class CategoriasComponent implements OnInit {

  private readonly api = inject(AdminService);
  private readonly auth = inject(AuthService);
  readonly links = ADMIN_LINKS;

  readonly empresa = computed(() => this.auth.usuario()?.empresaNombre ?? 'Panel del negocio');
  readonly categorias = signal<Categoria[]>([]);
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly aviso = signal('');
  readonly guardando = signal(false);
  readonly franja = signal<Categoria | null>(null);
  /** Errores de la franja: dentro del modal, porque el de la página queda tapado. */
  readonly errorFranja = signal('');
  readonly previaFranja = signal('');
  nueva = '';
  datos = {titulo: '', frase: '', nota: ''};
  private fotoFranja: File | null = null;

  ngOnInit(): void {
    this.api.categorias().subscribe({
      next: c => {
        this.categorias.set(c);
        this.cargando.set(false);
      },
      error: err => {
        this.error.set(apiMessage(err));
        this.cargando.set(false);
      }
    });
  }

  agregar(): void {
    const nombre = this.nueva.trim();
    if (!nombre) return;
    this.api.crearCategoria(nombre).subscribe({
      next: c => {
        this.categorias.update(l => [...l, c]);
        this.nueva = '';
        this.avisar(`Categoría «${c.nombre}» agregada.`);
      },
      error: err => this.error.set(apiMessage(err))
    });
  }

  renombrar(c: Categoria, input: HTMLInputElement): void {
    const nombre = input.value.trim();
    if (!nombre || nombre === c.nombre) {
      input.value = c.nombre;
      return;
    }
    this.guardar({...c, nombre}, () => input.value = c.nombre);
  }

  visible(c: Categoria, e: Event): void {
    const input = e.target as HTMLInputElement;
    this.guardar({...c, activa: input.checked}, () => input.checked = !input.checked);
  }

  soltar(e: CdkDragDrop<Categoria[]>): void {
    if (e.previousIndex === e.currentIndex) return;
    this.mover(e.previousIndex, e.currentIndex);
  }

  /** Mover con el teclado: flechas arriba y abajo sobre el asa. */
  teclado(e: KeyboardEvent, i: number): void {
    const destino = e.key === 'ArrowUp' ? i - 1 : e.key === 'ArrowDown' ? i + 1 : -1;
    if (destino < 0 || destino >= this.categorias().length) return;
    e.preventDefault();
    this.mover(i, destino);
    // El foco sigue al asa de la categoría movida.
    setTimeout(() => (document.querySelectorAll<HTMLElement>('.cdk-drag-handle')[destino])?.focus());
  }

  eliminar(c: Categoria): void {
    if (!confirm(`¿Eliminar la categoría «${c.nombre}»?`)) return;
    this.api.eliminarCategoria(c.id).subscribe({
      next: () => {
        this.categorias.update(l => l.filter(x => x.id !== c.id));
        this.avisar(`Categoría «${c.nombre}» eliminada.`);
      },
      error: err => this.error.set(apiMessage(err))
    });
  }

  abrirFranja(c: Categoria): void {
    this.datos = {titulo: c.bannerTitulo ?? c.nombre, frase: c.bannerFrase ?? '', nota: c.bannerNota ?? ''};
    this.fotoFranja = null;
    this.previaFranja.set('');
    this.error.set('');
    this.errorFranja.set('');
    this.franja.set(c);
  }

  otraConFranja(c: Categoria): string | null {
    return this.categorias().find(x => x.bannerActivo && x.id !== c.id)?.nombre ?? null;
  }

  async elegirFoto(e: Event): Promise<void> {
    const input = e.target as HTMLInputElement, elegida = input.files?.[0];
    input.value = '';
    if (!elegida) return;
    let f: File;
    try {
      f = await prepararFoto(elegida);
    } catch (err) {
      this.errorFranja.set((err as Error).message);
      return;
    }
    this.errorFranja.set('');
    this.fotoFranja = f;
    this.previaFranja.set(URL.createObjectURL(f));
  }

  guardarFranja(c: Categoria): void {
    this.guardando.set(true);
    this.api.guardarFranja(c.id, this.datos, this.fotoFranja).subscribe({
      next: () => {
        this.guardando.set(false);
        this.franja.set(null);
        this.avisar(`«${c.nombre}» sale como franja en el inicio.`);
        this.ngOnInit();
      },
      error: err => {
        this.guardando.set(false);
        this.errorFranja.set(apiMessage(err));
      }
    });
  }

  quitarFranja(c: Categoria): void {
    this.api.quitarFranja(c.id).subscribe({
      next: actualizada => {
        this.categorias.update(l => l.map(x => x.id === c.id ? actualizada : x));
        this.franja.set(null);
        this.avisar('Franja quitada.');
      },
      error: err => this.errorFranja.set(apiMessage(err))
    });
  }

  private mover(desde: number, hasta: number): void {
    const antes = this.categorias(), lista = [...antes];
    moveItemInArray(lista, desde, hasta);
    this.categorias.set(lista);
    this.api.ordenarCategorias(lista.map(c => c.id)).subscribe({
      next: c => this.categorias.set(c),
      error: err => {
        this.categorias.set(antes);
        this.error.set(apiMessage(err));
      }
    });
  }

  private guardar(c: Categoria, deshacer: () => void): void {
    this.api.actualizarCategoria(c).subscribe({
      next: actualizada => this.categorias.update(l => l.map(x => x.id === c.id ? actualizada : x)),
      error: err => {
        deshacer();
        this.error.set(apiMessage(err));
      }
    });
  }

  private avisar(m: string): void {
    this.error.set('');
    this.aviso.set(m);
  }
}
