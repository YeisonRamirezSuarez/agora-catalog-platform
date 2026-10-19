import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {CdkDrag, CdkDragDrop, CdkDragHandle, CdkDropList, moveItemInArray} from '@angular/cdk/drag-drop';
import {FormsModule} from '@angular/forms';

import {apiMessage, AuthService} from '@app/core/services/auth.service';
import {PanelShellComponent} from '@app/shared/layouts/panel-shell/panel-shell.component';
import {ADMIN_LINKS, AdminService, Coleccion, Producto} from '../admin.service';

const MAXIMO = 6;
const MAX_FOTO = 5 * 1024 * 1024;

interface Borrador {
  id?: number;
  nombre: string;
  descripcion: string;
  visible: boolean;
  productos: number[];
  imagenUrl: string | null;
}

const sinTildes = (s: string) => s.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');

/** Colecciones de «¿Cuál es el plan?»: textos de la sección, orden, productos y foto de cada plan. */
@Component({
  selector: 'app-colecciones',
  imports: [PanelShellComponent, FormsModule, CdkDropList, CdkDrag, CdkDragHandle],
  template: `
    <app-panel-shell [subtitle]="empresa()" [links]="links">
      <header class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 class="page-title">Colecciones</h1>
          <p class="mt-1 max-w-2xl text-muted">Los planes de la sección «{{ seccion.titulo || '¿Cuál es el plan?' }}» del inicio: cada uno abre el catálogo con sus productos. Hasta {{ maximo }}.</p>
        </div>
        @if (colecciones().length < maximo && !borrador()) {
          <button type="button" class="btn btn-dark" (click)="nueva()">+ Nueva colección</button>
        }
      </header>

      @if (error()) { <p class="alert-error mt-6" role="alert">{{ error() }}</p> }
      @if (aviso()) { <p class="alert-ok mt-6" role="status">{{ aviso() }}</p> }

      <form class="card mt-6 grid gap-4 p-5 sm:grid-cols-3" (ngSubmit)="guardarSeccion()">
        <h2 class="text-lg font-semibold sm:col-span-3">Textos de la sección</h2>
        <div class="field"><label for="s-tit">Título</label><input class="input" id="s-tit" name="titulo" [(ngModel)]="seccion.titulo" maxlength="40"></div>
        <div class="field"><label for="s-tex">Texto</label><input class="input" id="s-tex" name="texto" [(ngModel)]="seccion.texto" maxlength="80"></div>
        <div class="field"><label for="s-not">Nota escrita a mano</label><input class="input" id="s-not" name="nota" [(ngModel)]="seccion.nota" maxlength="40" placeholder="Siempre queda bien."></div>
        <div class="sm:col-span-3"><button class="btn">Guardar textos</button></div>
      </form>

      @if (borrador(); as b) {
        <form class="card mt-6 grid gap-5 p-5 lg:grid-cols-[minmax(0,1fr)_280px]" (ngSubmit)="guardar(b)">
          <div class="grid content-start gap-4">
            <h2 class="text-lg font-semibold">{{ b.id ? 'Editar colección' : 'Nueva colección' }}</h2>
            <div class="field"><label for="k-nom">Nombre</label><input class="input" id="k-nom" name="nombre" [(ngModel)]="b.nombre" maxlength="40" placeholder="Una buena picada"></div>
            <div class="field"><label for="k-des">Descripción</label><input class="input" id="k-des" name="descripcion" [(ngModel)]="b.descripcion" maxlength="80" placeholder="Para compartir con los amigos."></div>
            <label class="inline-flex items-center gap-3 font-semibold"><input type="checkbox" class="switch" role="switch" name="visible" [(ngModel)]="b.visible"> Visible en el catálogo</label>
            <div class="grid gap-2">
              <p class="font-semibold">Productos <span class="font-normal text-muted">({{ b.productos.length }} elegidos)</span></p>
              <input class="input" type="search" name="buscar" [(ngModel)]="buscar" placeholder="Buscar producto" aria-label="Buscar producto">
              <ul class="max-h-[360px] overflow-y-auto rounded-xl border border-line">
                @for (p of filtrados(); track p.id) {
                  <li class="border-b border-line last:border-0">
                    <label class="flex cursor-pointer items-center gap-3 px-3 py-2 hover:bg-wash">
                      <input type="checkbox" class="size-[18px] accent-[#1C1814]" [checked]="b.productos.includes(p.id)" (change)="alternar(b, p.id)">
                      @if (p.imagenThumbUrl) { <img [src]="p.imagenThumbUrl" alt="" class="size-10 rounded-lg object-cover"> }
                      @else { <span class="size-10 rounded-lg bg-wash"></span> }
                      <span><b class="block text-sm">{{ p.nombre }}</b><span class="text-xs text-muted">{{ p.categoriaNombre }}</span></span>
                    </label>
                  </li>
                } @empty { <li class="px-3 py-6 text-center text-sm text-muted">Ningún producto con esa búsqueda.</li> }
              </ul>
            </div>
          </div>
          <div class="grid content-start gap-3">
            <p class="font-semibold">Foto</p>
            @if (previa() || b.imagenUrl) { <img [src]="previa() || b.imagenUrl" alt="" class="aspect-[4/3] w-full rounded-xl object-cover"> }
            @else { <div class="grid aspect-[4/3] w-full place-items-center rounded-xl bg-wash text-sm text-faint">Sin foto</div> }
            <label class="btn cursor-pointer">{{ previa() || b.imagenUrl ? 'Cambiar foto' : 'Elegir foto' }}
              <input type="file" class="sr-only" accept="image/jpeg,image/png,image/webp" (change)="elegirFoto($event)"></label>
            <div class="mt-2 flex flex-wrap gap-2">
              <button class="btn btn-dark" [disabled]="guardando()">{{ guardando() ? 'Guardando…' : 'Guardar' }}</button>
              <button type="button" class="btn" (click)="cerrar()">Cancelar</button>
            </div>
            @if (b.id) { <button type="button" class="btn-link mt-2 justify-self-start text-sm text-danger" (click)="eliminar(b)">Eliminar colección</button> }
          </div>
        </form>
      }

      <ol class="card mt-6 divide-y divide-line" cdkDropList (cdkDropListDropped)="soltar($event)">
        @for (k of colecciones(); track k.id) {
          <li class="flex flex-wrap items-center gap-3 bg-white px-4 py-3" cdkDrag cdkDragLockAxis="y">
            <button type="button" class="grid size-9 cursor-grab place-items-center rounded-lg text-faint hover:bg-wash" cdkDragHandle [attr.aria-label]="'Mover ' + k.nombre">⋮⋮</button>
            @if (k.imagenUrl) { <img [src]="k.imagenUrl" alt="" class="size-14 rounded-lg object-cover"> } @else { <span class="size-14 rounded-lg bg-wash"></span> }
            <div class="min-w-0 flex-1">
              <b>{{ k.nombre }}</b>
              <p class="text-sm text-muted">{{ k.productos.length === 1 ? '1 producto' : k.productos.length + ' productos' }}@if (!k.productos.length) { · no sale en el catálogo hasta que tenga productos}</p>
            </div>
            <span class="badge" [class]="k.visible ? 'bg-ok-soft text-ok-ink' : 'bg-wash text-muted'">{{ k.visible ? 'Visible' : 'Oculta' }}</span>
            <button type="button" class="btn min-h-9 px-3 text-sm" (click)="editar(k)">Editar</button>
          </li>
        } @empty {
          <li class="px-4 py-10 text-center text-muted">{{ cargando() ? 'Cargando…' : 'Todavía no tienes colecciones.' }}</li>
        }
      </ol>
    </app-panel-shell>
  `
})
export class ColeccionesComponent implements OnInit {

  private readonly api = inject(AdminService);
  private readonly auth = inject(AuthService);
  readonly links = ADMIN_LINKS;
  readonly maximo = MAXIMO;

  readonly empresa = computed(() => this.auth.usuario()?.empresaNombre ?? 'Panel del negocio');
  readonly colecciones = signal<Coleccion[]>([]);
  readonly productos = signal<Producto[]>([]);
  readonly borrador = signal<Borrador | null>(null);
  readonly previa = signal('');
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly aviso = signal('');
  readonly guardando = signal(false);
  seccion = {titulo: '', texto: '', nota: ''};
  buscar = '';
  private foto: File | null = null;

  filtrados(): Producto[] {
    const q = sinTildes(this.buscar.trim());
    return q ? this.productos().filter(p => sinTildes(`${p.nombre} ${p.codigo} ${p.categoriaNombre}`).includes(q)) : this.productos();
  }

  ngOnInit(): void {
    this.api.colecciones().subscribe({
      next: c => { this.colecciones.set(c); this.cargando.set(false); },
      error: err => { this.error.set(apiMessage(err)); this.cargando.set(false); }
    });
    this.api.configuracion().subscribe({
      next: c => this.seccion = {titulo: c.coleccionesTitulo, texto: c.coleccionesTexto ?? '', nota: c.coleccionesNota ?? ''},
      error: () => undefined
    });
    this.api.todosLosProductos().subscribe({next: p => this.productos.set(p), error: () => undefined});
  }

  guardarSeccion(): void {
    if (!this.seccion.titulo.trim()) {
      this.error.set('Escribe el título de la sección');
      return;
    }
    this.api.guardarSeccionColecciones(this.seccion).subscribe({
      next: () => this.avisar('Textos de la sección guardados.'),
      error: err => this.error.set(apiMessage(err))
    });
  }

  nueva(): void {
    this.abrir({nombre: '', descripcion: '', visible: true, productos: [], imagenUrl: null});
  }

  editar(k: Coleccion): void {
    this.abrir({id: k.id, nombre: k.nombre, descripcion: k.descripcion ?? '', visible: k.visible, productos: [...k.productos], imagenUrl: k.imagenUrl});
  }

  alternar(b: Borrador, id: number): void {
    b.productos = b.productos.includes(id) ? b.productos.filter(x => x !== id) : [...b.productos, id];
  }

  elegirFoto(e: Event): void {
    const input = e.target as HTMLInputElement, f = input.files?.[0];
    input.value = '';
    if (!f) return;
    if (f.size > MAX_FOTO) {
      this.error.set('La foto pesa más de 5 MB. Elige una más liviana.');
      return;
    }
    this.foto = f;
    this.previa.set(URL.createObjectURL(f));
  }

  guardar(b: Borrador): void {
    if (!b.nombre.trim()) {
      this.error.set('Escribe el nombre de la colección');
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    this.api.guardarColeccion({...b, nombre: b.nombre.trim()}).subscribe({
      next: k => {
        if (!this.foto) return this.listo(k, !b.id);
        this.api.fotoColeccion(k.id, this.foto).subscribe({
          next: conFoto => this.listo(conFoto, !b.id),
          error: err => { this.listo(k, !b.id); this.error.set(`La colección quedó guardada, pero la foto no: ${apiMessage(err)}`); }
        });
      },
      error: err => { this.guardando.set(false); this.error.set(apiMessage(err)); }
    });
  }

  eliminar(b: Borrador): void {
    if (!b.id || !confirm(`¿Eliminar la colección «${b.nombre}»? Los productos no se borran.`)) return;
    this.api.eliminarColeccion(b.id).subscribe({
      next: () => {
        this.colecciones.update(l => l.filter(k => k.id !== b.id));
        this.cerrar();
        this.avisar('Colección eliminada.');
      },
      error: err => this.error.set(apiMessage(err))
    });
  }

  soltar(e: CdkDragDrop<Coleccion[]>): void {
    if (e.previousIndex === e.currentIndex) return;
    const antes = this.colecciones(), lista = [...antes];
    moveItemInArray(lista, e.previousIndex, e.currentIndex);
    this.colecciones.set(lista);
    this.api.ordenarColecciones(lista.map(k => k.id)).subscribe({
      next: c => this.colecciones.set(c),
      error: err => { this.colecciones.set(antes); this.error.set(apiMessage(err)); }
    });
  }

  cerrar(): void {
    this.borrador.set(null);
    this.previa.set('');
    this.foto = null;
  }

  private abrir(b: Borrador): void {
    this.error.set('');
    this.aviso.set('');
    this.buscar = '';
    this.foto = null;
    this.previa.set('');
    this.borrador.set(b);
  }

  private listo(k: Coleccion, nueva: boolean): void {
    this.guardando.set(false);
    this.colecciones.update(l => nueva ? [...l, k] : l.map(x => x.id === k.id ? k : x));
    this.cerrar();
    this.avisar(nueva ? `Colección «${k.nombre}» creada.` : 'Colección guardada.');
  }

  private avisar(m: string): void {
    this.error.set('');
    this.aviso.set(m);
  }
}
