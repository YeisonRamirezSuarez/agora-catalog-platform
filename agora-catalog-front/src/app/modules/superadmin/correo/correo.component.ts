import {Component, inject, OnInit, signal} from '@angular/core';
import {DatePipe} from '@angular/common';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';

import {apiMessage, AuthService} from '@app/core/services/auth.service';
import {PasswordToggleComponent} from '@app/shared/components/password-toggle/password-toggle.component';
import {PanelShellComponent} from '@app/shared/layouts/panel-shell/panel-shell.component';
import {SUPERADMIN_LINKS} from '../superadmin-links';
import {CorreoEnvio, CorreoService, ProveedorCorreo, ResultadoEnvio, Seguridad} from './correo.service';

/** Atajos para los proveedores más comunes: solo llenan servidor, puerto y seguridad. */
const ATAJOS: {nombre: string; host: string; puerto: number; seguridad: Seguridad}[] = [
  {nombre: 'Gmail', host: 'smtp.gmail.com', puerto: 587, seguridad: 'STARTTLS'},
  {nombre: 'Outlook / Microsoft 365', host: 'smtp.office365.com', puerto: 587, seguridad: 'STARTTLS'},
];

@Component({
  selector: 'app-correo',
  imports: [PanelShellComponent, ReactiveFormsModule, PasswordToggleComponent, DatePipe],
  template: `
    <app-panel-shell subtitle="Super admin" [links]="links">
      <header>
        <h1 class="page-title">Correo</h1>
        <p class="mt-1 max-w-2xl text-muted">
          Cuenta desde la que la plataforma envía los correos, como el usuario y la contraseña de cada negocio nuevo.
        </p>
      </header>

      @if (error()) {
        <p class="alert-error mt-6" role="alert">{{ error() }}</p>
      }
      @if (guardado()) {
        <p class="alert-ok mt-6" role="status">Proveedor guardado.</p>
      }

      <form [formGroup]="form" (ngSubmit)="guardar()" novalidate class="card mt-6 grid gap-5 p-5 sm:p-6">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <h2 class="text-lg font-semibold">Servidor SMTP</h2>
          <div class="flex flex-wrap gap-2">
            @for (a of atajos; track a.nombre) {
              <button type="button" class="btn min-h-9 px-3 text-sm" (click)="usar(a)">{{ a.nombre }}</button>
            }
          </div>
        </div>

        <div class="grid gap-4 sm:grid-cols-[1fr_120px_180px]">
          <div class="field">
            <label for="c-host">Servidor</label>
            <input class="input" id="c-host" formControlName="host" placeholder="smtp.gmail.com" autocomplete="off">
          </div>
          <div class="field">
            <label for="c-puerto">Puerto</label>
            <input class="input" id="c-puerto" type="number" formControlName="puerto" min="1" max="65535">
          </div>
          <div class="field">
            <label for="c-seg">Seguridad</label>
            <select class="input" id="c-seg" formControlName="seguridad">
              <option value="STARTTLS">STARTTLS (587)</option>
              <option value="SSL">SSL (465)</option>
              <option value="NINGUNA">Ninguna</option>
            </select>
          </div>
        </div>

        <div class="grid gap-4 sm:grid-cols-2">
          <div class="field">
            <label for="c-usuario">Usuario</label>
            <input class="input" id="c-usuario" formControlName="usuario" placeholder="cuenta@gmail.com" autocomplete="off">
          </div>
          <div class="field">
            <label for="c-pass">Contraseña</label>
            <div class="relative">
              <input #clave class="input pr-12" id="c-pass" type="password" formControlName="password"
                     autocomplete="new-password" aria-describedby="h-pass"
                     [placeholder]="proveedor()?.passwordGuardada ? '•••••••• (guardada)' : ''">
              <app-password-toggle [input]="clave" />
            </div>
            <p class="hint" id="h-pass">
              @if (proveedor()?.passwordGuardada) { Déjala vacía para conservar la guardada. }
              En Gmail usa una contraseña de aplicación, no la de la cuenta.
            </p>
          </div>
        </div>

        <div class="grid gap-4 sm:grid-cols-2">
          <div class="field">
            <label for="c-rem">Correo remitente</label>
            <input class="input" id="c-rem" type="email" formControlName="remitenteEmail" placeholder="cuenta@gmail.com">
          </div>
          <div class="field">
            <label for="c-nombre">Nombre del remitente</label>
            <input class="input" id="c-nombre" formControlName="remitenteNombre">
          </div>
        </div>

        <label class="inline-flex items-center gap-3 font-semibold">
          <input type="checkbox" class="switch" role="switch" formControlName="activo">
          Proveedor activo
        </label>

        <div>
          <button class="btn btn-dark" [disabled]="guardando()">{{ guardando() ? 'Guardando…' : 'Guardar' }}</button>
        </div>
      </form>

      <section class="card mt-6 grid gap-4 p-5 sm:p-6">
        <h2 class="text-lg font-semibold">Enviar un correo de prueba</h2>
        <div class="flex flex-wrap items-end gap-3">
          <div class="field min-w-[240px] flex-1">
            <label for="c-dest">Para</label>
            <input class="input" id="c-dest" type="email" [value]="destino()" (input)="destino.set($any($event.target).value)">
          </div>
          <button type="button" class="btn" [disabled]="probando() || !proveedor()" (click)="probar()">
            {{ probando() ? 'Enviando…' : 'Enviar prueba' }}
          </button>
        </div>
        @if (!proveedor()) {
          <p class="hint">Guarda primero el proveedor.</p>
        }
        @if (prueba(); as p) {
          <p [class]="p.enviado ? 'alert-ok' : 'alert-error'" role="status">
            {{ p.enviado ? 'Correo enviado. Revisa la bandeja de entrada (y el spam).' : 'No salió: ' + p.error }}
          </p>
        }
      </section>

      <section class="mt-6">
        <h2 class="text-lg font-semibold">Últimos envíos</h2>
        <div class="card mt-3 overflow-x-auto">
          <table class="table min-w-[640px]">
            <thead><tr><th>Fecha</th><th>Tipo</th><th>Para</th><th>Estado</th></tr></thead>
            <tbody>
              @for (e of envios(); track e.id) {
                <tr>
                  <td class="whitespace-nowrap text-muted">{{ e.createdAt | date: 'd MMM y, h:mm a' : undefined : 'es-CO' }}</td>
                  <td>{{ tipos[e.tipo] ?? e.tipo }}</td>
                  <td>{{ e.destinatario }}</td>
                  <td>
                    @if (e.enviado) {
                      <span class="badge bg-ok-soft text-ok-ink">Enviado</span>
                    } @else {
                      <span class="badge bg-danger-soft text-danger-ink">Falló</span>
                      <p class="mt-1 max-w-md text-xs text-muted">{{ e.error }}</p>
                    }
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="4" class="py-8 text-center text-muted">Todavía no se ha enviado ningún correo.</td></tr>
              }
            </tbody>
          </table>
        </div>
      </section>
    </app-panel-shell>
  `
})
export class CorreoComponent implements OnInit {

  private readonly api = inject(CorreoService);
  readonly links = SUPERADMIN_LINKS;
  readonly atajos = ATAJOS;
  readonly tipos: Record<string, string | undefined> ={BIENVENIDA: 'Bienvenida', PRUEBA: 'Prueba'};

  readonly proveedor = signal<ProveedorCorreo | null>(null);
  readonly envios = signal<CorreoEnvio[]>([]);
  readonly error = signal('');
  readonly guardando = signal(false);
  readonly guardado = signal(false);
  readonly probando = signal(false);
  readonly prueba = signal<ResultadoEnvio | null>(null);
  readonly destino = signal(inject(AuthService).usuario()?.email ?? '');

  readonly form = inject(FormBuilder).nonNullable.group({
    host: ['', Validators.required],
    puerto: [587, [Validators.required, Validators.min(1), Validators.max(65535)]],
    seguridad: ['STARTTLS' as Seguridad],
    usuario: [''],
    password: [''],
    remitenteEmail: ['', [Validators.required, Validators.email]],
    remitenteNombre: ['Agorá Catalog', Validators.required],
    activo: [true]
  });

  ngOnInit(): void {
    this.api.get().subscribe({
      next: p => {
        this.proveedor.set(p);
        if (p) this.form.patchValue({...p, usuario: p.usuario ?? '', password: ''});
      },
      error: err => this.error.set(apiMessage(err))
    });
    this.cargarEnvios();
  }

  usar(atajo: {host: string; puerto: number; seguridad: Seguridad}): void {
    this.form.patchValue({host: atajo.host, puerto: atajo.puerto, seguridad: atajo.seguridad});
  }

  guardar(): void {
    this.form.markAllAsTouched();
    this.guardado.set(false);
    if (this.form.invalid) {
      this.error.set(this.form.controls.remitenteEmail.invalid
        ? 'Escribe un correo remitente válido'
        : 'Completa el servidor, el puerto y el nombre del remitente');
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    this.api.save(this.form.getRawValue()).subscribe({
      next: p => {
        this.proveedor.set(p);
        this.form.controls.password.setValue('');
        this.guardado.set(true);
        this.guardando.set(false);
      },
      error: err => {
        this.error.set(apiMessage(err));
        this.guardando.set(false);
      }
    });
  }

  probar(): void {
    this.probando.set(true);
    this.prueba.set(null);
    this.api.probar(this.destino()).subscribe({
      next: r => {
        this.prueba.set(r);
        this.probando.set(false);
        this.cargarEnvios();
      },
      error: err => {
        this.prueba.set({enviado: false, error: apiMessage(err)});
        this.probando.set(false);
      }
    });
  }

  private cargarEnvios(): void {
    this.api.envios().subscribe({next: e => this.envios.set(e), error: () => undefined});
  }
}
