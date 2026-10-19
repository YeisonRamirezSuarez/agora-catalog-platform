import {Component, computed, inject, signal} from '@angular/core';
import {HttpErrorResponse} from '@angular/common/http';

import {apiMessage, AuthService} from '@app/core/services/auth.service';
import {PanelShellComponent} from '@app/shared/layouts/panel-shell/panel-shell.component';
import {ADMIN_LINKS, AdminService, ReporteImagenes, ReporteImportacion} from '../admin.service';

/**
 * Carga masiva: productos desde Excel (revisar y después guardar, todo o nada) y fotos en un ZIP
 * nombradas con el código del producto.
 */
@Component({
  selector: 'app-importar',
  imports: [PanelShellComponent],
  template: `
    <app-panel-shell [subtitle]="empresa()" [links]="links">
      <header>
        <h1 class="page-title">Importar</h1>
        <p class="mt-1 text-muted">Carga o actualiza muchos productos de una vez desde Excel, y sus fotos en un ZIP.</p>
      </header>

      <section class="card mt-6 grid gap-4 p-5">
        <h2 class="text-lg font-semibold">Productos desde Excel</h2>
        <ol class="grid gap-1 pl-5 text-sm text-muted [list-style:decimal]">
          <li>Descarga la plantilla (o tus productos actuales para editarlos).</li>
          <li>Llénala en Excel o Google Sheets y guárdala como .xlsx.</li>
          <li>Súbela: te mostramos qué se va a crear y qué se va a actualizar antes de guardar.</li>
        </ol>
        <div class="flex flex-wrap gap-2">
          <button type="button" class="btn" (click)="descargar('plantilla')" [disabled]="ocupado()">Descargar plantilla</button>
          <button type="button" class="btn" (click)="descargar('exportar')" [disabled]="ocupado()">Exportar mis productos</button>
          <label class="btn btn-dark cursor-pointer" [class.pointer-events-none]="ocupado()">
            Subir Excel
            <input type="file" class="sr-only" accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                   (change)="elegirExcel($event)">
          </label>
        </div>

        @if (errorExcel()) {
          <p class="alert-error" role="alert">{{ errorExcel() }}</p>
        }

        @if (reporte(); as r) {
          @if (r.aplicado) {
            <p class="alert-ok" role="status">
              Listo: {{ r.nuevos }} {{ r.nuevos === 1 ? 'producto creado' : 'productos creados' }} y
              {{ r.actualizados }} {{ r.actualizados === 1 ? 'actualizado' : 'actualizados' }}.
              @if (r.categoriasNuevas.length) { Categorías nuevas: {{ r.categoriasNuevas.join(', ') }}. }
            </p>
          } @else {
            <div class="grid gap-3 rounded-xl border border-line p-4">
              <p class="text-sm"><b>{{ archivoExcel()?.name }}</b>: {{ r.filas }} {{ r.filas === 1 ? 'fila' : 'filas' }}.</p>
              @if (r.errores.length) {
                <p class="alert-error" role="alert">
                  {{ r.errores.length === 1 ? 'Una fila tiene errores' : r.errores.length + ' filas tienen errores' }}.
                  No se guarda nada hasta que los corrijas y vuelvas a subir el archivo.
                </p>
                <div class="max-h-96 overflow-auto rounded-xl border border-line">
                  <table class="table">
                    <thead><tr><th class="w-20">Fila</th><th>Qué corregir</th></tr></thead>
                    <tbody>
                      @for (e of r.errores; track e.fila) {
                        <tr><td class="tabular-nums">{{ e.fila }}</td><td>{{ e.mensaje }}</td></tr>
                      }
                    </tbody>
                  </table>
                </div>
              } @else {
                <ul class="grid gap-1 text-sm">
                  <li>Productos nuevos: <b>{{ r.nuevos }}</b></li>
                  <li>Productos que se actualizan: <b>{{ r.actualizados }}</b></li>
                  @if (r.categoriasNuevas.length) {
                    <li>Categorías nuevas: <b>{{ r.categoriasNuevas.join(', ') }}</b></li>
                  }
                </ul>
                <div class="flex flex-wrap gap-2">
                  <button type="button" class="btn btn-dark" (click)="confirmar()" [disabled]="ocupado()">
                    {{ ocupado() ? 'Guardando…' : 'Guardar ' + (r.nuevos + r.actualizados) + ' productos' }}
                  </button>
                  <button type="button" class="btn" (click)="limpiarExcel()" [disabled]="ocupado()">Cancelar</button>
                </div>
              }
            </div>
          }
        }
      </section>

      <section class="card mt-6 grid gap-4 p-5">
        <h2 class="text-lg font-semibold">Fotos en un ZIP</h2>
        <p class="text-sm text-muted">
          Nombra cada foto con el código del producto (QUE-001.jpg va al producto QUE-001) y súbelas juntas en un ZIP.
          JPG, PNG o WebP de hasta 5 MB cada una; el ZIP, hasta 50 MB. Si el producto ya tenía foto, se reemplaza.
        </p>
        <label class="btn btn-dark cursor-pointer justify-self-start" [class.pointer-events-none]="subiendoZip()">
          {{ subiendoZip() ? 'Subiendo fotos…' : 'Subir ZIP' }}
          <input type="file" class="sr-only" accept=".zip,application/zip" (change)="elegirZip($event)">
        </label>

        @if (errorZip()) {
          <p class="alert-error" role="alert">{{ errorZip() }}</p>
        }
        @if (fotos(); as f) {
          <div class="grid gap-3 text-sm">
            <p [class]="f.asignadas.length ? 'alert-ok' : 'alert-error'" role="status">
              {{ f.asignadas.length }} {{ f.asignadas.length === 1 ? 'foto asignada' : 'fotos asignadas' }}.
            </p>
            @if (f.sinProducto.length) {
              <div>
                <p class="font-semibold">Sin producto con ese código ({{ f.sinProducto.length }})</p>
                <p class="mt-1 text-muted">{{ f.sinProducto.join(', ') }}</p>
              </div>
            }
            @if (f.conError.length) {
              <div>
                <p class="font-semibold">No se pudieron usar ({{ f.conError.length }})</p>
                <ul class="mt-1 grid gap-0.5 text-muted">
                  @for (e of f.conError; track $index) {
                    <li><b class="text-ink">{{ e.archivo }}</b>: {{ e.mensaje }}</li>
                  }
                </ul>
              </div>
            }
          </div>
        }
      </section>
    </app-panel-shell>
  `
})
export class ImportarComponent {

  private readonly api = inject(AdminService);
  private readonly auth = inject(AuthService);
  readonly links = ADMIN_LINKS;
  readonly empresa = computed(() => this.auth.usuario()?.empresaNombre ?? 'Panel del negocio');

  readonly archivoExcel = signal<File | null>(null);
  readonly reporte = signal<ReporteImportacion | null>(null);
  readonly errorExcel = signal('');
  readonly ocupado = signal(false);

  readonly fotos = signal<ReporteImagenes | null>(null);
  readonly errorZip = signal('');
  readonly subiendoZip = signal(false);

  descargar(cual: 'plantilla' | 'exportar'): void {
    this.errorExcel.set('');
    this.api.descargarExcel(cual).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = cual === 'plantilla' ? 'plantilla-productos.xlsx' : 'productos.xlsx';
        a.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
      },
      error: () => this.errorExcel.set('No pudimos descargar el archivo. Intenta de nuevo.')
    });
  }

  elegirExcel(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const archivo = input.files?.[0];
    input.value = ''; // permite volver a subir el mismo archivo después de corregirlo
    if (!archivo) return;
    this.archivoExcel.set(archivo);
    this.enviar('previsualizar');
  }

  confirmar(): void {
    this.enviar('confirmar');
  }

  limpiarExcel(): void {
    this.archivoExcel.set(null);
    this.reporte.set(null);
  }

  private enviar(paso: 'previsualizar' | 'confirmar'): void {
    const archivo = this.archivoExcel();
    if (!archivo) return;
    this.ocupado.set(true);
    this.errorExcel.set('');
    this.api.importarExcel(paso, archivo).subscribe({
      next: r => { this.reporte.set(r); this.ocupado.set(false); },
      error: (err: unknown) => {
        this.ocupado.set(false);
        // 422: el archivo cambió entre revisar y guardar; llega el reporte con los errores.
        if (err instanceof HttpErrorResponse && err.status === 422 && err.error?.errores) this.reporte.set(err.error);
        else { this.reporte.set(null); this.errorExcel.set(apiMessage(err)); }
      }
    });
  }

  elegirZip(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const archivo = input.files?.[0];
    input.value = '';
    if (!archivo) return;
    this.subiendoZip.set(true);
    this.errorZip.set('');
    this.fotos.set(null);
    this.api.fotosZip(archivo).subscribe({
      next: r => { this.fotos.set(r); this.subiendoZip.set(false); },
      error: err => { this.errorZip.set(apiMessage(err)); this.subiendoZip.set(false); }
    });
  }
}
