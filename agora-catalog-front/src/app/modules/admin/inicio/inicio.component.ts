import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {RouterLink} from '@angular/router';
import QRCode from 'qrcode';

import {apiMessage, AuthService} from '@app/core/services/auth.service';
import {PanelShellComponent} from '@app/shared/layouts/panel-shell/panel-shell.component';
import {ADMIN_LINKS, AdminService, Dashboard} from '../admin.service';

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
    </app-panel-shell>
  `
})
export class InicioComponent implements OnInit {

  private readonly api = inject(AdminService);
  private readonly auth = inject(AuthService);
  readonly links = ADMIN_LINKS;
  readonly d = signal<Dashboard | null>(null);
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
  }

  copiar(): void {
    navigator.clipboard?.writeText(this.link()).then(() => this.copiado.set(true), () => undefined);
  }
}
