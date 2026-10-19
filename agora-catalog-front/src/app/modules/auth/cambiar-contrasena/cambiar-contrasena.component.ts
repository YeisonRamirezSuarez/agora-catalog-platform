import {Component, inject, signal} from '@angular/core';
import {AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators} from '@angular/forms';
import {Router} from '@angular/router';

import {apiMessage, AuthService} from '@app/core/services/auth.service';
import {PasswordToggleComponent} from '@app/shared/components/password-toggle/password-toggle.component';

const iguales = (group: AbstractControl): ValidationErrors | null =>
  group.get('nueva')?.value === group.get('confirmar')?.value ? null : {distintas: true};

@Component({
  selector: 'app-cambiar-contrasena',
  imports: [ReactiveFormsModule, PasswordToggleComponent],
  template: `
    <main class="panel-surface grid place-items-center px-4 py-6">
      <form [formGroup]="form" (ngSubmit)="submit()" novalidate class="card grid w-full max-w-[420px] gap-[18px] px-7 py-8">
        <img src="assets/images/brand/logo.svg" alt="Agorá Catalog" width="123" height="26">
        <div>
          <h1 class="page-title">Crea tu contraseña</h1>
          <p class="mt-1 text-muted">
            @if (auth.usuario()?.debeCambiarPassword) {
              Entraste con una contraseña temporal. Elige una propia para seguir.
            } @else {
              Cambia la contraseña con la que entras al panel.
            }
          </p>
        </div>

        @if (error()) {
          <p class="alert-error" role="alert">{{ error() }}</p>
        }

        <div class="field">
          <label for="f-actual">Contraseña actual</label>
          <div class="relative">
            <input #actual class="input pr-12" id="f-actual" type="password" formControlName="actual" autocomplete="current-password">
            <app-password-toggle [input]="actual" />
          </div>
        </div>
        <div class="field">
          <label for="f-nueva">Contraseña nueva</label>
          <div class="relative">
            <input #nueva class="input pr-12" id="f-nueva" type="password" formControlName="nueva" autocomplete="new-password"
                   aria-describedby="h-nueva">
            <app-password-toggle [input]="nueva" />
          </div>
          <p class="hint" id="h-nueva">Mínimo 8 caracteres.</p>
        </div>
        <div class="field">
          <label for="f-confirmar">Repite la contraseña nueva</label>
          <div class="relative">
            <input #confirmar class="input pr-12" id="f-confirmar" type="password" formControlName="confirmar"
                   autocomplete="new-password" aria-describedby="e-confirmar">
            <app-password-toggle [input]="confirmar" />
          </div>
          <p class="err" id="e-confirmar" aria-live="polite">
            {{ form.hasError('distintas') && form.controls.confirmar.touched ? 'Las dos contraseñas no coinciden' : '' }}
          </p>
        </div>

        <button class="btn btn-dark w-full" [disabled]="loading()">{{ loading() ? 'Guardando…' : 'Guardar contraseña' }}</button>
        <button type="button" class="btn-link justify-self-center text-sm" (click)="auth.logout()">Salir</button>
      </form>
    </main>
  `
})
export class CambiarContrasenaComponent {

  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly form = inject(FormBuilder).nonNullable.group({
    actual: ['', Validators.required],
    nueva: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    confirmar: ['', Validators.required]
  }, {validators: iguales});
  readonly loading = signal(false);
  readonly error = signal('');

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.error.set(this.form.controls.nueva.invalid
        ? 'La contraseña nueva debe tener entre 8 y 72 caracteres'
        : this.form.hasError('distintas') ? 'Las dos contraseñas no coinciden' : 'Completa los tres campos');
      return;
    }
    this.loading.set(true);
    this.error.set('');
    const {actual, nueva} = this.form.getRawValue();
    this.auth.changePassword(actual, nueva).subscribe({
      next: () => void this.router.navigateByUrl(this.auth.home()),
      error: err => {
        this.error.set(apiMessage(err));
        this.loading.set(false);
      }
    });
  }
}
