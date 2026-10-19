import {Component, input, signal} from '@angular/core';

/**
 * Ojo para ver u ocultar una contraseña. Va junto al input, dentro de un contenedor `relative`,
 * y el input lleva `pr-12` para que el texto no quede debajo del botón:
 *
 *   <div class="relative">
 *     <input #clave class="input pr-12" type="password">
 *     <app-password-toggle [input]="clave" />
 *   </div>
 */
@Component({
  selector: 'app-password-toggle',
  host: {class: 'absolute inset-y-0 right-0 flex items-center pr-1'},
  template: `
    <button type="button" (click)="toggle()" [attr.aria-pressed]="visible()"
            [attr.aria-label]="visible() ? 'Ocultar contraseña' : 'Mostrar contraseña'"
            [attr.title]="visible() ? 'Ocultar contraseña' : 'Mostrar contraseña'"
            class="grid size-10 place-items-center rounded-lg text-muted transition hover:bg-wash hover:text-ink
                   focus-visible:outline focus-visible:outline-2 focus-visible:outline-[#8E5F18]">
      <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"
           stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        @if (visible()) {
          <path d="M10.6 5.1A10 10 0 0 1 12 5c6.5 0 10 7 10 7a17 17 0 0 1-2.2 3.1M6.6 6.6A17 17 0 0 0 2 12s3.5 7 10 7a9.7 9.7 0 0 0 5.4-1.6"/>
          <path d="M9.9 9.9a3 3 0 0 0 4.2 4.2M3 3l18 18"/>
        } @else {
          <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/>
          <circle cx="12" cy="12" r="3"/>
        }
      </svg>
    </button>
  `
})
export class PasswordToggleComponent {
  readonly input = input.required<HTMLInputElement>();
  readonly visible = signal(false);

  toggle(): void {
    this.visible.update(v => !v);
    this.input().type = this.visible() ? 'text' : 'password';
    this.input().focus();
  }
}
