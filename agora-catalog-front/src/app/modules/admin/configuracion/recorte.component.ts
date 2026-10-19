import {Component, computed, ElementRef, input, output, signal, viewChild} from '@angular/core';

export interface Rect {
  x: number;
  y: number;
  ancho: number;
  alto: number;
}

/**
 * Editor de recorte: marco fijo con la proporción final; se arrastra la foto para encuadrar y se acerca
 * con la barra, la rueda, + / − o las flechas. Devuelve el rectángulo en píxeles de la foto original.
 * El logo puede alejarse hasta que entre completo (lo que sobra queda blanco); la portada no se sale.
 */
@Component({
  selector: 'app-recorte',
  host: {style: 'display:contents', '(document:keydown)': 'tecla($event)'},
  template: `
    <div class="fixed inset-0 z-50 grid place-items-center bg-black/60 p-4" (click)="$event.target === $event.currentTarget && cancelar.emit()">
      <div class="card grid w-full max-w-2xl gap-4 p-5" role="dialog" aria-modal="true" [attr.aria-label]="titulo()">
        <h2 class="text-xl font-semibold">{{ titulo() }}</h2>
        <div #marco class="relative w-full touch-none select-none overflow-hidden rounded-xl bg-white outline outline-1 outline-line"
             [style.aspect-ratio]="proporcion()" [class.rounded-3xl]="puedeSalirse()"
             (pointerdown)="empezar($event)" (pointermove)="mover($event)" (pointerup)="soltar()" (pointercancel)="soltar()"
             (wheel)="rueda($event)" tabindex="0" aria-label="Arrastra para encuadrar; flechas para mover, + y − para acercar">
          <img [src]="src()" alt="" draggable="false" (load)="cargada($event)"
               class="pointer-events-none absolute left-0 top-0 max-w-none origin-top-left"
               [style.width.px]="natural().w * escala()" [style.height.px]="natural().h * escala()"
               [style.transform]="'translate(' + pos().x + 'px,' + pos().y + 'px)'">
          @if (franjaCelular()) {
            <!-- Lo que se ve detrás del texto en el celular: en computador se ve la portada completa. -->
            <div class="pointer-events-none absolute inset-y-0 left-[34.7%] right-[34.7%] border-x-2 border-dashed border-white/90"></div>
          }
        </div>
        @if (franjaCelular()) { <p class="hint">Entre las líneas punteadas está lo que se ve en el celular, detrás del texto.</p> }
        <label class="flex items-center gap-3 text-sm font-semibold">Zoom
          <input type="range" class="flex-1 accent-[#1C1814]" [min]="zoomMin()" max="4" step="0.01" [value]="zoom()"
                 (input)="acercar(+$any($event.target).value)" aria-label="Zoom">
        </label>
        <div class="flex flex-wrap gap-2">
          <button type="button" class="btn btn-dark" (click)="guardar.emit(rect())">Guardar</button>
          <button type="button" class="btn" (click)="otra.emit()">Elegir otra foto</button>
          <button type="button" class="btn text-danger" (click)="quitar.emit()">Quitar</button>
          <button type="button" class="btn-link" (click)="cancelar.emit()">Cancelar</button>
        </div>
      </div>
    </div>
  `
})
export class RecorteComponent {
  readonly titulo = input('Encuadrar foto');
  readonly src = input.required<string>();
  /** Ancho / alto del resultado (1 para el logo; 1600 / 754 para la portada). */
  readonly proporcion = input(1);
  readonly puedeSalirse = input(false);
  readonly franjaCelular = input(false);
  readonly inicial = input<Rect | null>(null);
  readonly guardar = output<Rect>();
  readonly otra = output<void>();
  readonly quitar = output<void>();
  readonly cancelar = output<void>();

  private readonly marco = viewChild.required<ElementRef<HTMLElement>>('marco');
  readonly natural = signal({w: 1, h: 1});
  readonly zoom = signal(1);
  readonly pos = signal({x: 0, y: 0});
  private arrastre: {x: number; y: number; px: number; py: number} | null = null;

  private ancho(): number {
    return this.marco().nativeElement.clientWidth;
  }

  private alto(): number {
    return this.ancho() / this.proporcion();
  }

  /** Escala que cubre el marco con zoom 1. */
  private base(): number {
    const n = this.natural();
    return Math.max(this.ancho() / n.w, this.alto() / n.h);
  }

  readonly escala = computed(() => this.zoom() * this.baseCache());
  private readonly baseCache = signal(1);

  /** El logo puede alejarse hasta verse completo; la portada siempre cubre el marco. */
  readonly zoomMin = computed(() => {
    if (!this.puedeSalirse()) return 1;
    const n = this.natural(), ancho = this.marcoAncho(), alto = ancho / this.proporcion();
    const contener = Math.min(ancho / n.w, alto / n.h);
    return Math.min(1, contener / this.baseCache());
  });
  private readonly marcoAncho = signal(1);

  cargada(e: Event): void {
    const img = e.target as HTMLImageElement;
    this.natural.set({w: img.naturalWidth, h: img.naturalHeight});
    this.marcoAncho.set(this.ancho());
    this.baseCache.set(this.base());
    const r = this.inicial();
    if (r && r.ancho > 0) {
      const s = this.ancho() / r.ancho;
      this.zoom.set(Math.max(this.zoomMin(), Math.min(4, s / this.baseCache())));
      this.pos.set({x: -r.x * this.escala(), y: -r.y * this.escala()});
    } else {
      this.zoom.set(1);
      const n = this.natural(), s = this.escala();
      this.pos.set({x: (this.ancho() - n.w * s) / 2, y: (this.alto() - n.h * s) / 2});
    }
    this.limitar();
  }

  empezar(e: PointerEvent): void {
    (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
    this.arrastre = {x: e.clientX, y: e.clientY, px: this.pos().x, py: this.pos().y};
  }

  mover(e: PointerEvent): void {
    if (!this.arrastre) return;
    this.pos.set({x: this.arrastre.px + e.clientX - this.arrastre.x, y: this.arrastre.py + e.clientY - this.arrastre.y});
    this.limitar();
  }

  soltar(): void {
    this.arrastre = null;
  }

  rueda(e: WheelEvent): void {
    e.preventDefault();
    this.acercar(this.zoom() * (e.deltaY < 0 ? 1.08 : 1 / 1.08));
  }

  tecla(e: KeyboardEvent): void {
    const paso = 10, p = this.pos();
    if (e.key === 'Escape') this.cancelar.emit();
    else if (e.key === '+' || e.key === '=') this.acercar(this.zoom() * 1.1);
    else if (e.key === '-') this.acercar(this.zoom() / 1.1);
    else if (e.key.startsWith('Arrow')) {
      e.preventDefault();
      this.pos.set({
        x: p.x + (e.key === 'ArrowLeft' ? paso : e.key === 'ArrowRight' ? -paso : 0),
        y: p.y + (e.key === 'ArrowUp' ? paso : e.key === 'ArrowDown' ? -paso : 0)
      });
      this.limitar();
    }
  }

  /** Acerca o aleja manteniendo fijo el centro del marco. */
  acercar(z: number): void {
    const antes = this.escala(), cx = this.ancho() / 2, cy = this.alto() / 2, p = this.pos();
    this.zoom.set(Math.max(this.zoomMin(), Math.min(4, z)));
    const k = this.escala() / antes;
    this.pos.set({x: cx - (cx - p.x) * k, y: cy - (cy - p.y) * k});
    this.limitar();
  }

  rect(): Rect {
    const s = this.escala(), p = this.pos();
    return {x: Math.round(-p.x / s), y: Math.round(-p.y / s), ancho: Math.round(this.ancho() / s), alto: Math.round(this.alto() / s)};
  }

  /** La portada siempre cubre el marco; el logo alejado queda centrado donde sobre espacio. */
  private limitar(): void {
    const n = this.natural(), s = this.escala(), W = this.ancho(), H = this.alto(), p = this.pos();
    const w = n.w * s, h = n.h * s;
    const ajustar = (v: number, tam: number, marco: number) =>
      tam <= marco ? (this.puedeSalirse() ? (marco - tam) / 2 : 0) : Math.min(0, Math.max(marco - tam, v));
    this.pos.set({x: ajustar(p.x, w, W), y: ajustar(p.y, h, H)});
  }
}
