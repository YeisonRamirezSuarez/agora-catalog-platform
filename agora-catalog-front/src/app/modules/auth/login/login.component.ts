import {Component, inject, signal} from '@angular/core';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router} from '@angular/router';

import {apiMessage, AuthService} from '@app/core/services/auth.service';
import {PasswordToggleComponent} from '@app/shared/components/password-toggle/password-toggle.component';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, PasswordToggleComponent],
  template: `
    <main class="panel-surface grid place-items-center px-4 py-6">
      <form [formGroup]="form" (ngSubmit)="submit()" novalidate
            class="card grid w-full max-w-[400px] gap-[18px] px-7 py-8 shadow-[0_1px_2px_rgba(28,24,20,.05),0_24px_48px_-32px_rgba(28,24,20,.35)]">
        <img src="assets/images/brand/logo.svg" alt="Agorá Catalog" width="123" height="26">
        <div>
          <h1 class="page-title">Ingresa a tu panel</h1>
          <p class="mt-1 text-muted">Administra tu catálogo, tus precios y tus ofertas.</p>
        </div>

        @if (error()) {
          <p class="alert-error" role="alert">{{ error() }}</p>
        }

        <div class="field">
          <label for="f-email">Correo</label>
          <input class="input" id="f-email" type="email" formControlName="email" autocomplete="username"
                 [attr.aria-invalid]="invalid('email')" aria-describedby="e-email">
          <p class="err" id="e-email" aria-live="polite">{{ invalid('email') ? 'Escribe un correo válido' : '' }}</p>
        </div>
        <div class="field">
          <label for="f-pass">Contraseña</label>
          <div class="relative">
            <input #clave class="input pr-12" id="f-pass" type="password" formControlName="password" autocomplete="current-password"
                   [attr.aria-invalid]="invalid('password')" aria-describedby="e-pass">
            <app-password-toggle [input]="clave" />
          </div>
          <p class="err" id="e-pass" aria-live="polite">{{ invalid('password') ? 'Escribe tu contraseña' : '' }}</p>
        </div>

        <button class="btn btn-dark w-full" [disabled]="loading()">{{ loading() ? 'Ingresando…' : 'Ingresar' }}</button>
      </form>
    </main>
  `
})
export class LoginComponent {

  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });
  readonly loading = signal(false);
  readonly error = signal('');

  invalid(control: 'email' | 'password'): boolean {
    const c = this.form.controls[control];
    return c.invalid && (c.touched || c.dirty);
  }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.loading()) return;
    this.loading.set(true);
    this.error.set('');
    const {email, password} = this.form.getRawValue();
    this.auth.login(email, password).subscribe({
      next: sesion => void this.router.navigateByUrl(this.auth.home(sesion.usuario)),
      error: err => {
        this.error.set(apiMessage(err, 'No pudimos conectar con el servidor. Intenta de nuevo.'));
        this.loading.set(false);
      }
    });
  }
}
