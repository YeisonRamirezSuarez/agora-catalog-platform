import {Component, computed, DestroyRef, effect, inject, signal, ViewEncapsulation} from '@angular/core';
import {toSignal} from '@angular/core/rxjs-interop';
import {ActivatedRoute, NavigationEnd, Router} from '@angular/router';
import {filter, map} from 'rxjs';

import {CatalogoStore} from './catalogo.store';
import {CatalogoPageComponent} from './catalogo-page.component';
import {HojasComponent} from './hojas.component';
import {InicioComponent} from './inicio.component';
import {CabeceraComponent, ItemMenu, PieComponent} from './marco.component';
import {IconoComponent} from './icono.component';

const NOCHE = '#15120D';

/**
 * Catálogo público de un negocio (/c/:publicId, /c/:publicId/catalogo, /c/:publicId/contacto).
 * Una sola página con el estado del catálogo: el pedido y los favoritos no se pierden al pasar del
 * inicio a la página de catálogo. Los estilos vienen del prototipo, encerrados bajo .cat-root.
 */
@Component({
  selector: 'app-catalogo-shell',
  imports: [CabeceraComponent, PieComponent, HojasComponent, InicioComponent, CatalogoPageComponent, IconoComponent],
  providers: [CatalogoStore],
  styleUrl: './catalogo.css',
  encapsulation: ViewEncapsulation.None,
  host: {
    class: 'cat-root',
    '[style.--brand]': 'colores()?.brand',
    '[style.--accent]': 'colores()?.accent',
    '(window:scroll)': 'alDesplazar()'
  },
  template: `
    @switch (store.estado()) {
      @case ('listo') {
        <div class="store" [class.home]="pagina() !== 'catalogo'">
          <cat-cabecera [solido]="pagina() === 'catalogo' || scrollY() > 24" [activo]="activo()" (buscar)="buscar()" />
          <main>
            @if (pagina() === 'catalogo') {
              <cat-catalogo [enfocar]="enfocar()" />
            } @else {
              <cat-inicio [irAContacto]="pagina() === 'contacto'" />
            }
          </main>
          <cat-pie />
        </div>
        <cat-hojas />
        <div id="toast" role="status" aria-live="polite" [class.show]="!!store.aviso()">{{ store.aviso() }}</div>
      }
      @case ('no-disponible') {
        <main class="na"><div class="na-box">
          <span class="na-icon"><svg catIcono="store"></svg></span>
          <h1>Catálogo no disponible</h1>
          <p>Este link no lleva a un catálogo activo. Pídele al negocio que te lo comparta de nuevo.</p>
          <img class="ec" src="assets/images/brand/logo.svg" alt="Agorá Catalog" width="123" height="26">
        </div></main>
      }
      @case ('error') {
        <main class="na"><div class="na-box">
          <span class="na-icon"><svg catIcono="store"></svg></span>
          <h1>No pudimos cargar el catálogo</h1>
          <p>Revisa tu conexión e intenta de nuevo.</p>
          <button type="button" class="btn" (click)="cargar()">Intentar de nuevo</button>
        </div></main>
      }
      @default {
        <main class="na" aria-busy="true"><p class="sr">Cargando el catálogo…</p></main>
      }
    }
  `
})
export class CatalogoShellComponent {
  readonly store = inject(CatalogoStore);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly scrollY = signal(0);
  readonly enfocar = signal(0);

  private readonly url = toSignal(this.router.events.pipe(
    filter(e => e instanceof NavigationEnd), map(() => this.router.url)), {initialValue: this.router.url});
  private readonly publicId = toSignal(this.route.paramMap.pipe(map(p => p.get('publicId') ?? '')), {initialValue: ''});

  /** inicio, catalogo o contacto, según la ruta. */
  readonly pagina = computed(() => {
    const path = this.url().split('?')[0];
    return path.endsWith('/catalogo') ? 'catalogo' : path.endsWith('/contacto') ? 'contacto' : 'inicio';
  });

  readonly activo = computed<ItemMenu['clave']>(() => {
    const pag = this.pagina();
    if (pag !== 'catalogo') return pag;
    const q = new URLSearchParams(this.url().split('?')[1] ?? ''), claves = [...q.keys()];
    const franja = this.store.franja();
    if (claves.length === 1 && q.get('promo') === '1') return 'promos';
    if (franja && claves.length === 1 && q.get('cat') === String(franja.categoriaId)) return 'combos';
    return 'catalogo';
  });

  readonly colores = computed(() => this.store.estado() === 'listo'
    ? {brand: this.store.negocio().colorPrimario, accent: this.store.negocio().colorSecundario} : null);

  constructor() {
    let cargado = '';
    effect(() => {
      // Solo el hash identifica el catálogo: corregir el slug en la URL no vuelve a pedirlo.
      const hash = this.publicId().split('-').pop() ?? '';
      if (hash && hash !== cargado) {
        cargado = hash;
        this.store.cargar(this.publicId());
      }
    });
    effect(() => {
      if (this.store.estado() !== 'listo') return;
      const n = this.store.negocio();
      document.title = this.pagina() === 'catalogo' ? `Catálogo de ${n.nombre}` : `${n.nombre}: catálogo`;
    });
    // Barra del navegador del celular en el color de la portada.
    const meta = document.querySelector<HTMLMetaElement>('meta[name="theme-color"]');
    const antes = meta?.content;
    if (meta) meta.content = NOCHE;
    // Cada página nueva empieza arriba (salvo Contáctanos, que baja sola a su sección).
    const sub = this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe(() => {
      if (this.pagina() !== 'contacto' && !this.store.hoja()) window.scrollTo({top: 0});
    });
    inject(DestroyRef).onDestroy(() => {
      sub.unsubscribe();
      if (meta && antes) meta.content = antes;
      document.body.style.overflow = '';
    });
  }

  alDesplazar(): void {
    this.scrollY.set(window.scrollY);
  }

  cargar(): void {
    this.store.cargar(this.publicId());
  }

  /** La lupa del encabezado: lleva a la página de catálogo con el buscador listo. */
  buscar(): void {
    if (this.pagina() === 'catalogo') {
      this.enfocar.update(v => v + 1);
      return;
    }
    void this.router.navigate([this.store.base(), 'catalogo']).then(() => this.enfocar.update(v => v + 1));
  }
}
