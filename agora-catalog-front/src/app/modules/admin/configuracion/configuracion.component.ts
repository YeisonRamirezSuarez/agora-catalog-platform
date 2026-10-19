import {Component, computed, ElementRef, inject, OnInit, signal, viewChild} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {DomSanitizer} from '@angular/platform-browser';

import {apiMessage, AuthService} from '@app/core/services/auth.service';
import {NegocioService} from '@app/core/services/negocio.service';
import {fachadaSvg} from '@app/modules/catalogo/fachadas';
import {PanelShellComponent} from '@app/shared/layouts/panel-shell/panel-shell.component';
import {prepararFoto} from '@app/shared/utils/foto';
import {ADMIN_LINKS, AdminService, Configuracion, contraste, Fachada, IconoSello, Recorte} from '../admin.service';
import {RecorteComponent} from './recorte.component';

const NOCHE = '#15120D';

const FACHADAS: {valor: Fachada; texto: string}[] = [
  {valor: 'SALSAMENTARIA', texto: 'Salsamentaria'}, {valor: 'PANADERIA', texto: 'Panadería'}, {valor: 'FRUTERIA', texto: 'Frutería'},
  {valor: 'CAFETERIA', texto: 'Cafetería'}, {valor: 'MINIMERCADO', texto: 'Minimercado'}, {valor: 'FERRETERIA', texto: 'Ferretería'},
];
const ICONOS: {valor: IconoSello; texto: string}[] = [
  {valor: 'CALIDAD', texto: 'Calidad'}, {valor: 'DOMICILIO', texto: 'Domicilio'}, {valor: 'WHATSAPP', texto: 'WhatsApp'},
  {valor: 'PRECIO', texto: 'Precio'}, {valor: 'TRADICION', texto: 'Tradición'}, {valor: 'HORARIO', texto: 'Horario'},
];

interface Edicion {
  tipo: 'logo' | 'portada';
  src: string;
  archivo: File | null;
  inicial: Recorte | null;
}

/** Configuración del catálogo: datos, WhatsApp, portada, logo, textos, sellos, dibujo del local, interruptores y colores. */
@Component({
  selector: 'app-configuracion',
  imports: [PanelShellComponent, FormsModule, RecorteComponent],
  template: `
    <app-panel-shell [subtitle]="empresa()" [links]="links">
      <header class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 class="page-title">Configuración</h1>
          <p class="mt-1 text-muted">Cómo se ve tu catálogo: textos, fotos, colores y lo que muestra.</p>
        </div>
        @if (cfg) { <a class="btn" [href]="cfg.linkPublico" target="_blank" rel="noopener">Ver mi catálogo</a> }
      </header>

      @if (error()) { <p class="alert-error mt-6" role="alert">{{ error() }}</p> }
      @if (aviso()) { <p class="alert-ok mt-6" role="status">{{ aviso() }}</p> }

      @if (cfg; as c) {
        <form class="mt-6 grid gap-6" (ngSubmit)="guardar()" novalidate>
          <!-- Vista previa de la portada: tocar la foto o el logo los cambia. -->
          <section class="card grid gap-3 p-5">
            <h2 class="text-lg font-semibold">Portada y logo</h2>
            <p class="text-sm text-muted">Toca la foto de portada o el logo para cambiarlos y encuadrarlos.</p>
            <div class="relative w-full overflow-hidden rounded-xl bg-[#15120D]" [style.aspect-ratio]="'1600 / 754'">
              <button type="button" class="absolute inset-0 cursor-pointer" (click)="editar('portada')" aria-label="Cambiar la foto de portada">
                @if (c.portadaUrl) { <img [src]="c.portadaUrl" alt="" class="absolute inset-0 size-full object-cover"> }
                <span class="absolute inset-0 bg-gradient-to-r from-[#15120D]/95 via-[#15120D]/60 to-[#15120D]/10"></span>
                @if (!c.portadaUrl) { <span class="absolute right-3 top-3 rounded-md bg-white/15 px-2 py-1 text-xs font-semibold text-white">Sin foto: toca para elegir</span> }
              </button>
              <div class="pointer-events-none absolute bottom-4 left-4 grid max-w-[70%] gap-1.5 text-white sm:gap-2">
                <p class="font-display text-base font-semibold leading-[1.1] sm:text-2xl sm:leading-tight">
                  @for (l of lineas(c.portadaTitulo || c.nombre); track $index) { @if (!$first) {<br>}{{ l }} }
                  @if (c.portadaTituloAcento) {
                    <em class="block" [style.color]="c.colorSecundario">@for (l of lineas(c.portadaTituloAcento); track $index) { @if (!$first) {<br>}{{ l }} }</em>
                  }
                </p>
                <span class="w-fit rounded px-2 py-0.5 text-[10px] font-bold sm:py-1 sm:text-[11px]" [style.background]="c.colorSecundario" style="color:#231A0E">Explorar el catálogo</span>
              </div>
              <button type="button" class="absolute left-3 top-3 size-10 overflow-hidden rounded-full sm:size-12 bg-white p-0.5 shadow" (click)="editar('logo')" aria-label="Cambiar el logo">
                @if (c.logoUrl) { <img [src]="c.logoUrl" alt="" class="size-full rounded-full object-cover"> }
                @else { <span class="grid size-full place-items-center rounded-full text-xs font-bold" [style.background]="c.colorPrimario" style="color:#fff">Logo</span> }
              </button>
            </div>
            <input #archivo type="file" class="sr-only" accept="image/jpeg,image/png,image/webp" (change)="elegida($event)">
          </section>

          <section class="card grid gap-4 p-5 sm:grid-cols-2">
            <h2 class="text-lg font-semibold sm:col-span-2">Datos del negocio</h2>
            <div class="field"><label for="c-nombre">Nombre</label><input class="input" id="c-nombre" name="nombre" [(ngModel)]="c.nombre" maxlength="120" required></div>
            <div class="field"><label for="c-slug">Link</label><input class="input" id="c-slug" name="slug" [(ngModel)]="c.slug" maxlength="80">
              <p class="hint">/c/{{ c.slug }}-{{ c.publicHash }}. Si lo cambias, el link viejo sigue funcionando.</p></div>
            <div class="field"><label for="c-corto">Nombre corto</label><input class="input" id="c-corto" name="nombreCorto" [(ngModel)]="c.nombreCorto" maxlength="24" placeholder="Javier">
              <p class="hint">Va grande en el encabezado y en «Nos vemos en…».</p></div>
            <div class="field"><label for="c-lema">Lema</label><input class="input" id="c-lema" name="lema" [(ngModel)]="c.lema" maxlength="32"></div>
            <div class="field sm:col-span-2"><label for="c-dir">Dirección</label><input class="input" id="c-dir" name="direccion" [(ngModel)]="c.direccion" maxlength="200"></div>
            <div class="field"><label for="c-hor">Horario</label><textarea class="input py-2" id="c-hor" name="horario" rows="2" [(ngModel)]="c.horario" maxlength="300"></textarea></div>
            <div class="field"><label for="c-dom">Domicilios</label><textarea class="input py-2" id="c-dom" name="infoDomicilios" rows="2" [(ngModel)]="c.infoDomicilios" maxlength="300"></textarea></div>
          </section>

          <section class="card grid gap-3 p-5">
            <h2 class="text-lg font-semibold">Números de WhatsApp</h2>
            <p class="text-sm text-muted">Con indicativo y solo números (ej. 573163218276). Hasta 3.</p>
            @for (w of c.whatsapps; track $index; let i = $index) {
              <div class="flex flex-wrap gap-2">
                <input class="input max-w-[220px]" [name]="'wa-num-' + i" [(ngModel)]="w.numero" inputmode="numeric" maxlength="15" placeholder="573001234567" [attr.aria-label]="'Número ' + (i + 1)">
                <input class="input flex-1" [name]="'wa-nom-' + i" [(ngModel)]="w.nombre" maxlength="40" placeholder="Nombre (opcional), ej. Pedidos" [attr.aria-label]="'Nombre del número ' + (i + 1)">
                <button type="button" class="btn" (click)="c.whatsapps.splice(i, 1)" [attr.aria-label]="'Quitar el número ' + (i + 1)">Quitar</button>
              </div>
            }
            @if (c.whatsapps.length < 3) {
              <button type="button" class="btn justify-self-start" (click)="c.whatsapps.push({numero: '', nombre: ''})">+ Agregar número</button>
            }
          </section>

          <section class="card grid gap-4 p-5 sm:grid-cols-2">
            <h2 class="text-lg font-semibold sm:col-span-2">Portada del catálogo</h2>
            <div class="field"><label for="c-ante">Antetítulo</label><input class="input" id="c-ante" name="portadaAntetitulo" [(ngModel)]="c.portadaAntetitulo" maxlength="40" placeholder="El sabor de reunirnos"></div>
            <div class="field"><label for="c-tit">Titular</label><input class="input" id="c-tit" name="portadaTitulo" [(ngModel)]="c.portadaTitulo" maxlength="40" [placeholder]="c.nombre"></div>
            <div class="field"><label for="c-acento">Línea en cursiva</label><textarea class="input py-2" id="c-acento" name="portadaTituloAcento" rows="2" [(ngModel)]="c.portadaTituloAcento" maxlength="40"></textarea>
              <p class="hint">Enter para partir la línea.</p></div>
            <div class="field"><label for="c-sub">Texto</label><textarea class="input py-2" id="c-sub" name="portadaSubtitulo" rows="2" [(ngModel)]="c.portadaSubtitulo" maxlength="140"></textarea></div>
            <div class="field"><label for="c-nota">Nota escrita a mano</label><textarea class="input py-2" id="c-nota" name="portadaNota" rows="2" [(ngModel)]="c.portadaNota" maxlength="48"></textarea>
              <p class="hint">Sale en computador, junto al titular.</p></div>
            <div class="field"><label for="c-sello">Texto del sello redondo</label><input class="input" id="c-sello" name="portadaSelloTexto" [(ngModel)]="c.portadaSelloTexto" maxlength="30" placeholder="Tradición · desde siempre"></div>
          </section>

          <section class="card grid gap-4 p-5 sm:grid-cols-3">
            <h2 class="text-lg font-semibold sm:col-span-3">Página de catálogo</h2>
            <div class="field"><label for="c-ctit">Titular</label><textarea class="input py-2" id="c-ctit" name="catalogoTitulo" rows="2" [(ngModel)]="c.catalogoTitulo" maxlength="40" placeholder="Tradición en cada"></textarea></div>
            <div class="field"><label for="c-cac">Final en cursiva</label><input class="input" id="c-cac" name="catalogoAcento" [(ngModel)]="c.catalogoAcento" maxlength="30" placeholder="bocado."></div>
            <div class="field"><label for="c-csub">Texto</label><textarea class="input py-2" id="c-csub" name="catalogoSubtitulo" rows="2" [(ngModel)]="c.catalogoSubtitulo" maxlength="140"></textarea></div>
          </section>

          <section class="card grid gap-3 p-5">
            <h2 class="text-lg font-semibold">Sellos debajo de la portada</h2>
            <p class="text-sm text-muted">Frases cortas con ícono. Hasta 4.</p>
            @for (s of c.sellos; track $index; let i = $index) {
              <div class="flex flex-wrap gap-2">
                <select class="input max-w-[170px]" [name]="'sello-ic-' + i" [(ngModel)]="s.icono" [attr.aria-label]="'Ícono del sello ' + (i + 1)">
                  @for (o of iconos; track o.valor) { <option [value]="o.valor">{{ o.texto }}</option> }
                </select>
                <input class="input flex-1" [name]="'sello-tx-' + i" [(ngModel)]="s.texto" maxlength="48" [attr.aria-label]="'Texto del sello ' + (i + 1)">
                <button type="button" class="btn" (click)="c.sellos.splice(i, 1)">Quitar</button>
              </div>
            }
            @if (c.sellos.length < 4) {
              <button type="button" class="btn justify-self-start" (click)="c.sellos.push({icono: 'CALIDAD', texto: ''})">+ Agregar sello</button>
            }
          </section>

          <section class="card grid gap-3 p-5">
            <h2 class="text-lg font-semibold">«Nos vemos en…»</h2>
            <div class="field max-w-md"><label for="c-vnota">Nota escrita a mano</label><textarea class="input py-2" id="c-vnota" rows="2" name="visitaNota" [(ngModel)]="c.visitaNota" maxlength="40" placeholder="El buen sabor de siempre"></textarea></div>
            <p class="text-sm font-semibold">Dibujo del local</p>
            <div class="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-7">
              <label class="grid cursor-pointer place-items-center gap-1 rounded-xl border border-line p-3 text-sm font-semibold has-[:checked]:border-ink has-[:checked]:ring-1 has-[:checked]:ring-ink">
                <input type="radio" class="sr-only" name="fachada" [value]="null" [(ngModel)]="c.fachada">
                <span class="grid aspect-[260/168] w-full place-items-center rounded-lg border border-dashed border-line text-xs text-faint">Sin dibujo</span>Ninguno
              </label>
              @for (f of fachadas; track f.valor) {
                <label class="grid cursor-pointer place-items-center gap-1 rounded-xl border border-line p-3 text-sm font-semibold text-[#7A5A2A] has-[:checked]:border-ink has-[:checked]:ring-1 has-[:checked]:ring-ink">
                  <input type="radio" class="sr-only" name="fachada" [value]="f.valor" [(ngModel)]="c.fachada">
                  <span class="w-full [&_svg]:h-auto [&_svg]:w-full" [innerHTML]="dibujo(f.valor)"></span>
                  <span class="text-ink">{{ f.texto }}</span>
                </label>
              }
            </div>
          </section>

          <section class="card grid gap-3 p-5">
            <h2 class="text-lg font-semibold">Qué muestra el catálogo</h2>
            <label class="inline-flex items-center gap-3"><input type="checkbox" class="switch" role="switch" name="mostrarPrecios" [(ngModel)]="c.mostrarPrecios"> Mostrar precios <span class="text-sm text-muted">(apagado, tampoco se ven las ofertas)</span></label>
            <label class="inline-flex items-center gap-3"><input type="checkbox" class="switch" role="switch" name="pedidosWhatsapp" [(ngModel)]="c.pedidosWhatsapp"> Pedido por WhatsApp <span class="text-sm text-muted">(apagado, cada producto tiene «Preguntar»)</span></label>
            <label class="inline-flex items-center gap-3"><input type="checkbox" class="switch" role="switch" name="mostrarAgotados" [(ngModel)]="c.mostrarAgotados"> Mostrar agotados con la etiqueta «Agotado»</label>
            <label class="inline-flex items-center gap-3"><input type="checkbox" class="switch" role="switch" name="adornos" [(ngModel)]="c.adornos"> Hojas de adorno junto a los títulos <span class="text-sm text-muted">(solo computador)</span></label>
          </section>

          <section class="card grid gap-4 p-5 sm:grid-cols-2">
            <h2 class="text-lg font-semibold sm:col-span-2">Colores</h2>
            <div class="field">
              <label for="c-prim">Color de botones</label>
              <div class="flex items-center gap-3">
                <input type="color" id="c-prim" name="colorPrimario" class="h-11 w-16 cursor-pointer rounded-lg border border-line-strong" [(ngModel)]="c.colorPrimario">
                <span class="rounded-lg px-3 py-2 text-sm font-semibold text-white" [style.background]="c.colorPrimario">Agregar al pedido</span>
              </div>
              @if (errorPrimario()) { <p class="err">{{ errorPrimario() }}</p> } @else { <p class="hint">Oscuro, para que el texto blanco se lea.</p> }
            </div>
            <div class="field">
              <label for="c-sec">Color de acento</label>
              <div class="flex items-center gap-3">
                <input type="color" id="c-sec" name="colorSecundario" class="h-11 w-16 cursor-pointer rounded-lg border border-line-strong" [(ngModel)]="c.colorSecundario">
                <span class="rounded-lg bg-[#15120D] px-3 py-2 font-display text-sm italic" [style.color]="c.colorSecundario">un buen plan.</span>
              </div>
              @if (errorAcento()) { <p class="err">{{ errorAcento() }}</p> } @else { <p class="hint">Claro, para que se lea sobre la portada oscura.</p> }
            </div>
          </section>

          <div class="sticky bottom-4 flex justify-end">
            <button class="btn btn-dark shadow-lg" [disabled]="guardando()">{{ guardando() ? 'Guardando…' : 'Guardar cambios' }}</button>
          </div>
        </form>
      } @else if (!error()) {
        <p class="mt-6 text-muted">Cargando…</p>
      }

      @if (edicion(); as e) {
        <app-recorte [titulo]="e.tipo === 'logo' ? 'Encuadrar el logo' : 'Encuadrar la portada'" [src]="e.src"
                     [proporcion]="e.tipo === 'logo' ? 1 : 1600 / 754" [puedeSalirse]="e.tipo === 'logo'"
                     [franjaCelular]="e.tipo === 'portada'" [inicial]="e.inicial"
                     (guardar)="guardarFoto(e, $event)" (otra)="elegirArchivo(e.tipo)" (quitar)="quitarFoto(e.tipo)" (cancelar)="edicion.set(null)" />
      }
    </app-panel-shell>
  `
})
export class ConfiguracionComponent implements OnInit {

  private readonly api = inject(AdminService);
  private readonly auth = inject(AuthService);
  private readonly negocio = inject(NegocioService);
  private readonly sanitizer = inject(DomSanitizer);
  readonly links = ADMIN_LINKS;
  readonly fachadas = FACHADAS;
  readonly iconos = ICONOS;
  private readonly archivoInput = viewChild<ElementRef<HTMLInputElement>>('archivo');

  readonly empresa = computed(() => this.auth.usuario()?.empresaNombre ?? 'Panel del negocio');
  cfg: Configuracion | null = null;
  readonly error = signal('');
  readonly aviso = signal('');
  readonly guardando = signal(false);
  readonly edicion = signal<Edicion | null>(null);
  private tipoPendiente: 'logo' | 'portada' = 'portada';

  ngOnInit(): void {
    this.api.configuracion().subscribe({
      next: c => this.cfg = {...c, whatsapps: c.whatsapps.map(w => ({...w})), sellos: c.sellos.map(s => ({...s}))},
      error: err => this.error.set(apiMessage(err))
    });
  }

  lineas(s: string | null): string[] {
    return (s ?? '').split('\n');
  }

  dibujo(f: Fachada) {
    return this.sanitizer.bypassSecurityTrustHtml(fachadaSvg(f, this.cfg?.nombre ?? ''));
  }

  errorPrimario(): string {
    return this.cfg && contraste(this.cfg.colorPrimario, '#FFFFFF') < 4.5
      ? 'El texto blanco casi no se lee sobre este color. Elige un tono más oscuro.' : '';
  }

  errorAcento(): string {
    return this.cfg && contraste(this.cfg.colorSecundario, NOCHE) < 4.5
      ? 'Este color casi no se lee sobre la portada oscura. Elige un tono más claro.' : '';
  }

  guardar(): void {
    const c = this.cfg;
    if (!c) return;
    if (this.errorPrimario() || this.errorAcento()) {
      this.error.set('Revisa los colores: así no se leen bien.');
      return;
    }
    const whatsapps = c.whatsapps.map(w => ({numero: w.numero.replace(/\D/g, ''), nombre: w.nombre?.trim() || null}))
      .filter(w => w.numero);
    const sellos = c.sellos.filter(s => s.texto.trim());
    this.guardando.set(true);
    this.error.set('');
    this.aviso.set('');
    this.api.guardarConfiguracion({...c, whatsapps, sellos}).subscribe({
      next: nueva => {
        this.cfg = {...nueva, whatsapps: nueva.whatsapps.map(w => ({...w})), sellos: nueva.sellos.map(s => ({...s}))};
        this.negocio.actualizar(nueva);
        this.guardando.set(false);
        this.aviso.set('Cambios guardados. Ya se ven en tu catálogo.');
      },
      error: err => {
        this.guardando.set(false);
        this.error.set(apiMessage(err));
      }
    });
  }

  /** Con foto guardada abre el editor con el original; sin foto, el selector de archivo. */
  editar(tipo: 'logo' | 'portada'): void {
    const c = this.cfg!;
    const original = tipo === 'logo' ? c.logoOriginalUrl : c.portadaOriginalUrl;
    if (original) {
      this.edicion.set({tipo, src: original, archivo: null, inicial: tipo === 'logo' ? c.logoRecorte : c.portadaRecorte});
    } else {
      this.elegirArchivo(tipo);
    }
  }

  elegirArchivo(tipo: 'logo' | 'portada'): void {
    this.tipoPendiente = tipo;
    this.archivoInput()?.nativeElement.click();
  }

  async elegida(e: Event): Promise<void> {
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
    this.edicion.set({tipo: this.tipoPendiente, src: URL.createObjectURL(f), archivo: f, inicial: null});
  }

  guardarFoto(e: Edicion, rect: Recorte): void {
    this.api.guardarImagenEmpresa(e.tipo, e.archivo, rect).subscribe({
      next: c => {
        this.actualizarFotos(c);
        this.edicion.set(null);
        this.aviso.set(e.tipo === 'logo' ? 'Logo guardado.' : 'Portada guardada.');
      },
      error: err => {
        this.edicion.set(null);
        this.error.set(apiMessage(err));
      }
    });
  }

  quitarFoto(tipo: 'logo' | 'portada'): void {
    this.api.quitarImagenEmpresa(tipo).subscribe({
      next: c => {
        this.actualizarFotos(c);
        this.edicion.set(null);
      },
      error: err => this.error.set(apiMessage(err))
    });
  }

  /** Solo las fotos: lo que el dueño esté escribiendo en el formulario no se pierde. */
  private actualizarFotos(c: Configuracion): void {
    this.negocio.actualizar({...c, nombre: this.negocio.negocio()?.nombre ?? c.nombre});
    if (!this.cfg) return;
    Object.assign(this.cfg, {
      logoUrl: c.logoUrl, logoOriginalUrl: c.logoOriginalUrl, logoRecorte: c.logoRecorte,
      portadaUrl: c.portadaUrl, portadaOriginalUrl: c.portadaOriginalUrl, portadaRecorte: c.portadaRecorte
    });
  }
}
