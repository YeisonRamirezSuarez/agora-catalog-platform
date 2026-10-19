import {afterNextRender, Component, computed, ElementRef, inject, input, signal, viewChild} from '@angular/core';
import {DomSanitizer} from '@angular/platform-browser';
import {Router, RouterLink} from '@angular/router';

import {IconoSello} from './catalogo.models';
import {CatalogoStore} from './catalogo.store';
import {lineas, MSG_GENERAL, phone, porRelevancia, resumenHorario, waHref} from './catalogo.util';
import {fachadaSvg, FLECHA_NOTA, OLA, selloSvg} from './fachadas';
import {IconoComponent, NombreIcono} from './icono.component';
import {FotoComponent, TarjetaComponent} from './piezas.component';

const ICONO_SELLO: Record<IconoSello, NombreIcono> = {
  CALIDAD: 'leafshield', DOMICILIO: 'moto', WHATSAPP: 'wa', PRECIO: 'shield', TRADICION: 'award', HORARIO: 'clock'
};

type Pestana = string; // 'todos' | 'ofertas' | 'mas' | 'c<id>'

/** Inicio del catálogo: portada, sellos, categorías, planes, favoritos, franja y contacto. */
@Component({
  selector: 'cat-inicio',
  imports: [RouterLink, IconoComponent, FotoComponent, TarjetaComponent],
  host: {style: 'display:contents'},
  template: `
    @let n = store.negocio();
    <section class="hero" aria-labelledby="hero-h">
      @if (n.portadaUrl) { <img [src]="n.portadaUrl" alt="" fetchpriority="high"> }
      @if (n.portadaAntetitulo) { <p class="eyebrow">{{ n.portadaAntetitulo }}</p> }
      <h1 id="hero-h">
        @for (l of lineas(n.portadaTitulo || n.nombre); track $index) { @if (!$first) {<br>}{{ l }} }
        @if (n.portadaTituloAcento) {
          <em>@for (l of lineas(n.portadaTituloAcento); track $index) { @if (!$first) {<br>}{{ l }} }</em>
        }
      </h1>
      @if (n.portadaSubtitulo) { <p class="hero-sub">{{ n.portadaSubtitulo }}</p> }
      <form class="hero-find" role="search" (submit)="buscar($event, hq.value)">
        <label class="sr" for="hq">Buscar productos</label>
        <input #hq id="hq" name="q" type="search" [placeholder]="store.pistaBusqueda()" enterkeyhint="search" autocomplete="off">
        <button aria-label="Buscar"><svg catIcono="search"></svg></button>
      </form>
      <a class="hero-cta" [routerLink]="[store.base(), 'catalogo']">Explorar el catálogo<svg catIcono="arrow"></svg></a>
      @if (n.portadaNota) {
        <p class="hero-note" aria-hidden="true">@for (l of lineas(n.portadaNota); track $index) { @if (!$first) {<br>}{{ l }} }<span [innerHTML]="svg(flechaNota)"></span></p>
      }
      <span [innerHTML]="sello()"></span>
    </section>

    @if (store.data()!.sellos.length) {
      <ul class="sellos">
        @for (s of store.data()!.sellos; track $index) {
          <li><svg [catIcono]="iconoSello[s.icono]"></svg><span>{{ s.texto }}</span></li>
        }
      </ul>
    }

    <div class="counter">
      <span [innerHTML]="svg(ola)"></span>
      <nav class="circles" aria-label="Categorías">
        @for (c of circulos(); track c.id) {
          <a class="circle" [routerLink]="[store.base(), 'catalogo']" [queryParams]="{cat: c.id}">
            <cat-foto [src]="c.foto" /><span>{{ c.nombre }}</span>
          </a>
        }
      </nav>

      @if (planes().length) {
        <section class="planes" aria-labelledby="pl-h">
          @if (n.adornos) {
            <img class="deco deco-laurel" src="catalogo/deco-laurel.webp" alt="" aria-hidden="true" loading="lazy" decoding="async">
            <img class="deco deco-perejil" src="catalogo/deco-perejil.webp" alt="" aria-hidden="true" loading="lazy" decoding="async">
          }
          @if (n.coleccionesNota) {
            <p class="side-note" aria-hidden="true">@for (l of lineas(n.coleccionesNota); track $index) { @if (!$first) {<br>}{{ l }} }</p>
          }
          <h2 class="sec-title" id="pl-h">{{ n.coleccionesTitulo }}</h2>
          @if (n.coleccionesTexto) { <p class="sec-sub">{{ n.coleccionesTexto }}</p> }
          <div class="plan-rail">
            @for (c of planes(); track c.id) {
              <a class="plan" [routerLink]="[store.base(), 'catalogo']" [queryParams]="{col: c.id}">
                @if (c.imagenUrl) { <img [src]="c.imagenUrl" alt="" loading="lazy"> } @else { <span class="ph-none"><svg catIcono="photo"></svg></span> }
                <span class="plan-txt">
                  <b>{{ c.nombre }}</b>
                  @if (c.descripcion) { <span class="plan-desc">{{ c.descripcion }}</span> }
                  <span class="plan-go" aria-hidden="true"><svg catIcono="arrow"></svg></span>
                </span>
              </a>
            }
          </div>
        </section>
      }

      <section class="favs" aria-labelledby="fv-h">
        <div class="favs-head">
          <h2 class="script" id="fv-h">Nuestros favoritos</h2>
          <div class="chips" role="group" aria-label="Elegir qué productos ver">
            @for (t of pestanas(); track t.k) {
              <button type="button" class="chip" [attr.aria-pressed]="t.k === pestana()" (click)="elegir(t.k)">{{ t.texto }}</button>
            }
          </div>
          <div class="favs-act">
            <a class="see-all" [routerLink]="[store.base(), 'catalogo']" [queryParams]="verTodos()">Ver todos<svg catIcono="arrow"></svg></a>
            <button type="button" class="rail-btn" aria-label="Productos anteriores" (click)="desplazar(-1)"><svg catIcono="back"></svg></button>
            <button type="button" class="rail-btn" aria-label="Más productos" (click)="desplazar(1)"><svg catIcono="chev"></svg></button>
          </div>
        </div>
        <div class="prow" #fila>
          @for (p of favoritos(); track p.id) { <cat-tarjeta [p]="p" /> }
        </div>
        @if (n.adornos) {
          <img class="deco deco-laurel-b" src="catalogo/deco-laurel.webp" alt="" aria-hidden="true" loading="lazy" decoding="async">
        }
      </section>
    </div>

    @if (store.franja(); as f) {
      <section class="band" aria-labelledby="bd-h">
        @if (f.imagenUrl) { <img [src]="f.imagenUrl" alt="" loading="lazy"> }
        <span [innerHTML]="svg(ola)"></span>
        @if (f.nota) { <p class="band-tag" aria-hidden="true">@for (l of lineas(f.nota); track $index) { @if (!$first) {<br>}{{ l }} }</p> }
        <div class="band-in">
          <h2 id="bd-h">@for (l of lineas(f.titulo || f.categoria.nombre); track $index) { @if (!$first) {<br>}{{ l }} }</h2>
          @if (f.frase) { <p>{{ f.frase }}</p> }
          <a class="hero-cta" [routerLink]="[store.base(), 'catalogo']" [queryParams]="{cat: f.categoriaId}">Ver {{ f.categoria.nombre.toLowerCase() }}<svg catIcono="arrow"></svg></a>
        </div>
      </section>
    }

    <section class="visit" id="contacto" aria-labelledby="vs-h" #contacto>
      <span [innerHTML]="svg(ola)"></span>
      <div class="v-in">
        @if (n.fachada) { <span [innerHTML]="fachada()"></span> }
        @if (n.visitaNota) { <p class="visit-note" aria-hidden="true">@for (l of lineas(n.visitaNota); track $index) { @if (!$first) {<br>}{{ l }} }</p> }
        <h2 id="vs-h">Nos vemos en <em>{{ n.nombreCorto || n.nombre }}.</em></h2>
        <p class="v-sub">{{ store.hayWhatsapp() ? 'Escríbenos por WhatsApp para pedir o preguntar, o pasa por el local.' : 'Pasa por el local; te atendemos con gusto.' }}</p>
        @if (store.hayWhatsapp()) {
          <div class="v-was">
            @for (w of store.whatsapps(); track w.numero) {
              <a class="v-wa" [href]="waHref(w.numero, msgGeneral)" target="_blank" rel="noopener">
                <span class="v-wa-ic"><svg catIcono="wa"></svg></span>
                <span><small>{{ w.nombre || 'WhatsApp' }}</small><b>{{ phone(w.numero) }}</b></span>
              </a>
            }
          </div>
        }
        <dl class="v-info">
          @if (n.direccion) {
            <div class="v-item"><dt>Dirección</dt><dd><span>{{ n.direccion }}</span>
              <a class="v-link" [href]="mapa()" target="_blank" rel="noopener">Cómo llegar<svg catIcono="arrow"></svg></a></dd></div>
          }
          @if (resumenHorario().length || n.horario) {
            <div class="v-item"><dt>Horario</dt><dd>
              @if (store.horarioAhora(); as e) { <span class="v-estado" [class.on]="e.abierto">{{ e.abierto ? 'Abierto' : 'Cerrado' }} · {{ e.texto }}</span> }
              @if (resumenHorario().length) { <span>@for (l of resumenHorario(); track $index) { @if (!$first) {<br>}{{ l }} }</span> }
              @if (n.horario) { <span>@for (l of frases(n.horario); track $index) { @if (!$first) {<br>}{{ l }} }</span> }
            </dd></div>
          }
          @if (n.infoDomicilios) {
            <div class="v-item"><dt>Domicilios</dt><dd><span>@for (l of frases(n.infoDomicilios); track $index) { @if (!$first) {<br>}{{ l }} }</span></dd></div>
          }
        </dl>
      </div>
    </section>
  `
})
export class InicioComponent {
  readonly store = inject(CatalogoStore);
  private readonly router = inject(Router);
  private readonly sanitizer = inject(DomSanitizer);
  /** Ruta /contacto: baja a la sección de contacto. */
  readonly irAContacto = input(false);

  readonly lineas = lineas;
  readonly phone = phone;
  readonly waHref = waHref;
  readonly msgGeneral = MSG_GENERAL;
  readonly iconoSello = ICONO_SELLO;
  readonly ola = OLA;
  readonly flechaNota = FLECHA_NOTA;
  readonly pestana = signal<Pestana>('todos');
  private readonly fila = viewChild<ElementRef<HTMLElement>>('fila');
  private readonly contacto = viewChild<ElementRef<HTMLElement>>('contacto');

  constructor() {
    afterNextRender(() => {
      if (this.irAContacto()) this.contacto()?.nativeElement.scrollIntoView({behavior: 'smooth'});
    });
  }

  svg(html: string) {
    return this.sanitizer.bypassSecurityTrustHtml(html);
  }

  readonly sello = computed(() => {
    const n = this.store.negocio();
    const letra = (n.nombreCorto || n.nombre).trim().split(/\s+/).pop()![0].toUpperCase();
    return this.svg(selloSvg(n.portadaSelloTexto || n.nombre, letra));
  });

  readonly fachada = computed(() => {
    const n = this.store.negocio();
    return n.fachada ? this.svg(fachadaSvg(n.fachada, n.nombre)) : '';
  });

  /** Círculos de categorías (celular): cada uno con la foto de un producto de la categoría. */
  readonly circulos = computed(() => this.store.categorias().map(c => {
    const p = this.store.productos().find(x => x.categoriaId === c.id && (x.imagenThumbUrl || x.imagenUrl));
    return {id: c.id, nombre: c.nombre, foto: c.imagenUrl || p?.imagenThumbUrl || p?.imagenUrl || null};
  }));

  readonly planes = computed(() => this.store.data()!.colecciones);

  readonly pestanas = computed(() => {
    const ps = this.store.productos();
    return [
      {k: 'todos', texto: 'Todos'},
      ...(this.store.precios() && this.store.enOferta().length ? [{k: 'ofertas', texto: 'Ofertas'}] : []),
      ...(ps.some(p => p.destacado) ? [{k: 'mas', texto: 'Más pedidos'}] : []),
      ...this.store.categorias().map(c => ({k: 'c' + c.id, texto: c.nombre})),
    ];
  });

  readonly favoritos = computed(() => {
    const k = this.pestana(), ps = this.store.productos();
    const lista = k === 'ofertas' ? this.store.enOferta()
      : k === 'mas' ? ps.filter(p => p.destacado)
      : k.startsWith('c') ? ps.filter(p => p.categoriaId === +k.slice(1))
      : porRelevancia(ps);
    return lista.slice(0, 12);
  });

  readonly verTodos = computed(() => {
    const k = this.pestana();
    return k === 'ofertas' ? {promo: '1'} : k === 'mas' ? {mas: '1'} : k.startsWith('c') ? {cat: k.slice(1)} : {};
  });

  readonly resumenHorario = computed(() => resumenHorario(this.store.data()!.horario));

  readonly mapa = computed(() => {
    const n = this.store.negocio();
    return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${n.nombre}, ${n.direccion}`)}`;
  });

  /** Cada frase en su línea, como en el prototipo ("Lunes a sábado... Domingos..."). */
  frases(s: string): string[] {
    return s.split('\n').flatMap(l => l.split(/(?<=\.) (?=[A-ZÁÉÍÓÚÑ])/));
  }

  elegir(k: Pestana): void {
    this.pestana.set(k);
    this.fila()?.nativeElement.scrollTo({left: 0});
  }

  desplazar(d: number): void {
    const el = this.fila()?.nativeElement;
    el?.scrollBy({left: d * el.clientWidth * 0.8, behavior: 'smooth'});
  }

  buscar(e: Event, q: string): void {
    e.preventDefault();
    const texto = q.trim();
    void this.router.navigate([this.store.base(), 'catalogo'], {queryParams: texto ? {q: texto} : {}});
  }
}
