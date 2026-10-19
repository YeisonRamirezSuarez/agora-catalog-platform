import {Component, inject, input} from '@angular/core';
import {RouterLink, RouterLinkActive} from '@angular/router';

import {AuthService} from '@app/core/services/auth.service';

export interface PanelLink {
  label: string;
  path: string;
}

/** Marco del panel: menú lateral en computador, barra superior en celular. */
@Component({
  selector: 'app-panel-shell',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <div class="panel-surface md:grid md:grid-cols-[240px_1fr]">
      <nav class="hidden border-r border-line bg-white md:flex md:min-h-dvh md:flex-col md:gap-1 md:p-4" aria-label="Menú">
        <div class="mb-4 grid gap-1 px-2 pt-2">
          <img src="assets/images/brand/logo.svg" alt="Agorá Catalog" width="123" height="26">
          <span class="text-sm text-muted">{{ subtitle() }}</span>
        </div>
        @for (link of links(); track link.path) {
          <a [routerLink]="link.path" routerLinkActive="bg-wash text-ink" [routerLinkActiveOptions]="{exact: true}"
             class="rounded-[10px] px-3 py-2.5 font-semibold text-ink-soft no-underline hover:bg-wash">{{ link.label }}</a>
        }
        <div class="flex-1"></div>
        <div class="grid gap-1 border-t border-line px-2 pt-3">
          <span class="truncate text-sm font-semibold">{{ auth.usuario()?.nombre }}</span>
          <span class="truncate text-xs text-muted">{{ auth.usuario()?.email }}</span>
          <button type="button" class="btn-link mt-2 justify-self-start text-sm" (click)="auth.logout()">Salir</button>
        </div>
      </nav>

      <header class="flex items-center justify-between border-b border-line bg-white px-4 py-2 md:hidden">
        <img src="assets/images/brand/logo.svg" alt="Agorá Catalog" width="110" height="23">
        <button type="button" class="btn-link inline-flex min-h-11 items-center px-1" (click)="auth.logout()">Salir</button>
      </header>

      <main class="mx-auto w-full max-w-6xl px-4 py-6 sm:px-8 sm:py-10">
        <ng-content />
      </main>
    </div>
  `
})
export class PanelShellComponent {
  readonly auth = inject(AuthService);
  readonly subtitle = input('');
  readonly links = input<PanelLink[]>([]);
}
