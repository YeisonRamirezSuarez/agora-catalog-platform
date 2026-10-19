import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {RouterLink} from '@angular/router';
import QRCode from 'qrcode';

import {apiMessage, AuthService} from '@app/core/services/auth.service';
import {PanelShellComponent} from '@app/shared/layouts/panel-shell/panel-shell.component';
import {ADMIN_LINKS, AdminService, Dashboard, Estadisticas} from '../admin.service';

/** Inicio del panel: cómo va el catálogo hoy y el link público con su QR. */
@Component({
  selector: 'app-inicio',
  imports: [PanelShellComponent, RouterLink],
  template: `
    <app-panel-shell [subtitle]="empresa()" [links]="links">
      <header class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 class="page-title">Hola, {{ nombre() }}</h1>
          <p class="mt-1 text-muted">Así va tu catálogo hoy.</p>
        </div>
        @if (d(); as d) {
          <a class="btn" [href]="d.linkPublico" target="_blank" rel="noopener">Ver mi catálogo</a>
        }
      </header>

      @if (error()) {
        <p class="alert-error mt-6" role="alert">{{ error() }}</p>
      }

      @if (d(); as d) {
        <div class="mt-6 grid gap-6 lg:grid-cols-2">
          <section class="card p-5">
            <h2 class="text-lg font-semibold">Tu catálogo</h2>
            <div class="mt-3 grid divide-y divide-line">
              <a class="flex items-center justify-between py-3 no-underline" routerLink="/admin/productos">
                <span>Productos</span><b class="tabular-nums">{{ d.productos }}</b></a>
              <a class="flex items-center justify-between py-3 no-underline" routerLink="/admin/productos">
                <span>Sin foto</span><b class="tabular-nums" [class.text-danger]="d.sinFoto > 0">{{ d.sinFoto }}</b></a>
              <a class="flex items-center justify-between py-3 no-underline" routerLink="/admin/productos" [queryParams]="{disponible: 'false'}">
                <span>Agotados</span><b class="tabular-nums">{{ d.agotados }}</b></a>
              <div class="flex items-center justify-between py-3"><span>Ofertas vigentes</span><b class="tabular-nums">{{ d.ofertasVigentes }}</b></div>
            </div>
          </section>

          <section class="card grid gap-4 p-5 sm:grid-cols-[1fr_auto]">
            <div class="grid content-start gap-3">
              <h2 class="text-lg font-semibold">Tu link público</h2>
              <p class="text-sm text-muted">Compártelo por WhatsApp o imprímelo como QR. Sigue funcionando aunque cambies el nombre del negocio.</p>
              <div class="flex gap-2">
                <input class="input" readonly [value]="link()" aria-label="Link público" (focus)="$any($event.target).select()">
                <button type="button" class="btn btn-dark" (click)="copiar()">{{ copiado() ? 'Copiado' : 'Copiar' }}</button>
              </div>
              @if (qr()) {
                <a class="btn justify-self-start" [href]="qr()" [download]="archivoQr()">Descargar QR</a>
              }
            </div>
            @if (qr()) {
              <img [src]="qr()" alt="Código QR del catálogo" width="160" height="160" class="justify-self-center rounded-xl border border-line">
            }
          </section>
        </div>
      } @else if (!error()) {
        <p class="mt-6 text-muted">Cargando…</p>
      }

      @if (est(); as e) {
        <section class="card mt-6 grid gap-6 p-5" aria-labelledby="est-h">
          <div class="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
            <h2 id="est-h" class="text-lg font-semibold">Tu catálogo en los últimos {{ e.dias }} días</h2>
            <p class="text-xs text-muted">Tus propias visitas con el panel abierto no cuentan.</p>
          </div>
          <dl class="grid grid-cols-3 gap-2 sm:gap-3">
            @for (t of totales(); track t.texto) {
              <div class="rounded-xl bg-wash px-3 py-3 sm:px-4">
                <dt class="text-xs text-muted sm:text-sm">{{ t.texto }}</dt>
                <dd class="font-display text-2xl font-semibold tabular-nums sm:text-3xl">{{ t.valor }}</dd>
              </div>
            }
          </dl>
          <div>
            <p class="mb-2 text-sm font-semibold">Visitas por día</p>
            <div class="flex h-28 items-end gap-[2px] border-b border-line" role="img" [attr.aria-label]="resumenDias()">
              @for (d of e.porDia; track d.fecha) {
                <div class="min-h-[2px] flex-1 rounded-t-sm bg-ink/80" [style.height.%]="maxVisitas() ? d.visitas / maxVisitas() * 100 : 0"
                     [title]="fechaCorta(d.fecha) + ': ' + d.visitas + (d.visitas === 1 ? ' visita' : ' visitas')"></div>
              }
            </div>
            <div class="mt-1 flex justify-between text-[11px] text-muted"><span>{{ fechaCorta(e.porDia[0].fecha) }}</span><span>Hoy</span></div>
          </div>
          <div>
            <p class="mb-2 text-sm font-semibold">Productos más vistos</p>
            @if (e.masVistos.length) {
              <ol class="grid gap-2">
                @for (p of e.masVistos; track p.id) {
                  <li class="flex items-center gap-3">
                    @if (p.imagenThumbUrl) { <img [src]="p.imagenThumbUrl" alt="" class="size-10 rounded-lg object-cover"> }
                    @else { <span class="size-10 rounded-lg bg-wash"></span> }
                    <span class="min-w-0 flex-1 truncate">{{ p.nombre }}</span>
                    <b class="tabular-nums">{{ p.vistas }}</b>
                  </li>
                }
              </ol>
            } @else {
              <p class="text-sm text-muted">Todavía nadie ha abierto un producto. Comparte tu link y aquí verás qué miran más.</p>
            }
          </div>
        </section>
      }
    </app-panel-shell>
  `
})
export class InicioComponent implements OnInit {

  private readonly api = inject(AdminService);
  private readonly auth = inject(AuthService);
  readonly links = ADMIN_LINKS;
  readonly d = signal<Dashboard | null>(null);
  readonly est = signal<Estadisticas | null>(null);
  readonly maxVisitas = computed(() => Math.max(0, ...(this.est()?.porDia ?? []).map(d => d.visitas)));
  readonly totales = computed(() => {
    const e = this.est();
    return e ? [{texto: 'Visitas', valor: e.visitas}, {texto: 'Pedidos enviados', valor: e.pedidos}, {texto: 'Clics a WhatsApp', valor: e.whatsapp}] : [];
  });
  readonly resumenDias = computed(() => {
    const e = this.est();
    return e ? `Visitas por día en los últimos ${e.dias} días: ${e.visitas} en total, máximo ${this.maxVisitas()} en un día.` : '';
  });
  readonly error = signal('');
  readonly qr = signal('');
  readonly copiado = signal(false);

  readonly empresa = computed(() => this.auth.usuario()?.empresaNombre ?? 'Panel del negocio');
  readonly nombre = computed(() => (this.auth.usuario()?.nombre ?? '').split(' ')[0]);
  readonly link = computed(() => this.d() ? location.origin + this.d()!.linkPublico : '');
  readonly archivoQr = computed(() => `qr-${this.d()?.linkPublico.split('/').pop() ?? 'catalogo'}.png`);

  ngOnInit(): void {
    this.api.dashboard().subscribe({
      next: d => {
        this.d.set(d);
        QRCode.toDataURL(this.link(), {width: 640, margin: 2, color: {dark: '#1C1814', light: '#FFFFFF'}})
          .then(url => this.qr.set(url), () => this.qr.set(''));
      },
      error: err => this.error.set(apiMessage(err))
    });
    // Si fallan, el inicio sigue sirviendo sin ellas.
    this.api.estadisticas(30).subscribe({next: e => this.est.set(e), error: () => undefined});
  }

  /** "2026-10-19" -> "19 oct". */
  fechaCorta(fecha: string): string {
    return new Date(fecha + 'T12:00:00').toLocaleDateString('es-CO', {day: 'numeric', month: 'short'}).replace('.', '');
  }

  copiar(): void {
    navigator.clipboard?.writeText(this.link()).then(() => this.copiado.set(true), () => undefined);
  }
}
