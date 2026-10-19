import {Component, inject, input} from '@angular/core';
import {RouterLink, RouterLinkActive} from '@angular/router';

import {AuthService} from '@app/core/services/auth.service';
import {NegocioService} from '@app/core/services/negocio.service';

export interface PanelLink {
  label: string;
  path: string;
}

/**
 * Marco del panel: menú lateral en computador; en celular, barra superior con el menú en una fila
 * que se desliza. En el panel de un negocio arriba van su logo y su nombre, y abajo la marca
 * Agorá Catalog sobre "Ver mi catálogo".
 */
@Component({
  selector: 'app-panel-shell',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <div class="panel-surface md:grid md:grid-cols-[240px_1fr]">
      <nav class="hidden border-r border-line bg-white md:sticky md:top-0 md:flex md:h-dvh md:flex-col md:gap-1 md:overflow-y-auto md:p-4" aria-label="Menú">
        @if (negocio.negocio(); as n) {
          <div class="mb-4 flex items-center gap-3 px-2 pt-2">
            <span class="grid size-11 shrink-0 place-items-center overflow-hidden rounded-xl border border-line bg-wash">
              @if (n.logoUrl) {
                <img [src]="n.logoUrl" alt="" width="44" height="44" class="size-full object-cover">
              } @else {
                <span class="font-display text-lg font-semibold">{{ n.nombre.charAt(0) }}</span>
              }
            </span>
            <span class="line-clamp-2 font-semibold leading-tight">{{ n.nombre }}</span>
          </div>
        } @else {
          <div class="mb-4 grid gap-1 px-2 pt-2">
            <img src="assets/images/brand/logo.svg" alt="Agorá Catalog" width="123" height="26">
            <span class="text-sm text-muted">{{ subtitle() }}</span>
          </div>
        }
        @for (link of links(); track link.path) {
          <a [routerLink]="link.path" routerLinkActive="bg-wash text-ink" [routerLinkActiveOptions]="{exact: true}"
             class="rounded-[10px] px-3 py-2.5 font-semibold text-ink-soft no-underline hover:bg-wash">{{ link.label }}</a>
        }
        <div class="flex-1"></div>
        @if (negocio.negocio(); as n) {
          <div class="grid gap-3 px-2 pb-3">
            <img src="assets/images/brand/logo.svg" alt="Agorá Catalog" width="104" height="22" class="opacity-80">
            <a class="btn justify-self-stretch" [href]="n.linkPublico" target="_blank" rel="noopener">Ver mi catálogo</a>
          </div>
        }
        <div class="grid gap-1 border-t border-line px-2 pt-3">
          <span class="truncate text-sm font-semibold">{{ auth.usuario()?.nombre }}</span>
          <span class="truncate text-xs text-muted">{{ auth.usuario()?.email }}</span>
          <button type="button" class="btn-link mt-2 justify-self-start text-sm" (click)="auth.logout()">Salir</button>
        </div>
      </nav>

      <header class="sticky top-0 z-30 border-b border-line bg-white md:hidden">
        <div class="flex items-center justify-between gap-3 px-4 py-2">
          @if (negocio.negocio(); as n) {
            <span class="flex min-w-0 items-center gap-2">
              @if (n.logoUrl) {
                <img [src]="n.logoUrl" alt="" width="32" height="32" class="size-8 shrink-0 rounded-lg border border-line object-cover">
              }
              <span class="truncate font-semibold">{{ n.nombre }}</span>
            </span>
          } @else {
            <img src="assets/images/brand/logo.svg" alt="Agorá Catalog" width="110" height="23">
          }
          <span class="flex shrink-0 items-center gap-3">
            @if (negocio.negocio(); as n) {
              <a class="btn-link inline-flex min-h-11 items-center text-sm" [href]="n.linkPublico" target="_blank" rel="noopener">Ver catálogo</a>
            }
            <button type="button" class="btn-link inline-flex min-h-11 items-center px-1" (click)="auth.logout()">Salir</button>
          </span>
        </div>
        @if (links().length > 1) {
          <nav class="flex gap-1 overflow-x-auto px-3 pb-2 [scrollbar-width:none]" aria-label="Menú">
            @for (link of links(); track link.path) {
              <a [routerLink]="link.path" routerLinkActive="bg-ink text-white" [routerLinkActiveOptions]="{exact: true}"
                 class="shrink-0 rounded-full px-3.5 py-2 text-sm font-semibold text-ink-soft no-underline">{{ link.label }}</a>
            }
          </nav>
        }
      </header>

      <main class="mx-auto w-full max-w-6xl px-4 py-6 sm:px-8 sm:py-10">
        <ng-content />
      </main>
    </div>
  `
})
export class PanelShellComponent {
  readonly auth = inject(AuthService);
  readonly negocio = inject(NegocioService);
  readonly subtitle = input('');
  readonly links = input<PanelLink[]>([]);

  constructor() {
    this.negocio.cargar();
  }
}
