import {Component, inject, OnInit, signal} from '@angular/core';
import {HttpClient} from '@angular/common/http';

import {apiMessage} from '@app/core/services/auth.service';
import {PanelShellComponent} from '@app/shared/layouts/panel-shell/panel-shell.component';

interface MiEmpresa {
  id: number;
  nombre: string;
  linkPublico: string;
  productos: number;
}

/** Inicio del panel del negocio. Productos, categorías y configuración llegan en la fase 5. */
@Component({
  selector: 'app-inicio',
  imports: [PanelShellComponent],
  template: `
    <app-panel-shell subtitle="Panel del negocio" [links]="[{label: 'Inicio', path: '/admin'}]">
      @if (empresa(); as e) {
        <h1 class="page-title">{{ e.nombre }}</h1>
        <div class="mt-6 grid gap-4 sm:grid-cols-2">
          <div class="card p-5">
            <p class="text-sm text-muted">Productos</p>
            <p class="mt-1 font-display text-4xl font-semibold">{{ e.productos }}</p>
          </div>
          <div class="card p-5">
            <p class="text-sm text-muted">Tu link público</p>
            <p class="mt-1 break-all font-semibold">{{ e.linkPublico }}</p>
          </div>
        </div>
        <p class="hint mt-6">Las pantallas de productos, categorías y configuración llegan en la próxima fase.</p>
      } @else if (error()) {
        <p class="alert-error" role="alert">{{ error() }}</p>
      } @else {
        <p class="text-muted">Cargando…</p>
      }
    </app-panel-shell>
  `
})
export class InicioComponent implements OnInit {

  private readonly http = inject(HttpClient);
  readonly empresa = signal<MiEmpresa | null>(null);
  readonly error = signal('');

  ngOnInit(): void {
    this.http.get<MiEmpresa>('/admin/empresa').subscribe({
      next: e => this.empresa.set(e),
      error: err => this.error.set(apiMessage(err))
    });
  }
}
