import {Component} from '@angular/core';
import {TestBed} from '@angular/core/testing';

import {PasswordToggleComponent} from './password-toggle.component';

@Component({
  imports: [PasswordToggleComponent],
  template: `<div class="relative"><input #clave type="password" value="secreta"><app-password-toggle [input]="clave" /></div>`
})
class HostComponent {
}

describe('PasswordToggleComponent', () => {

  it('muestra y oculta la contraseña, y el botón dice qué va a hacer', () => {
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input');
    const boton: HTMLButtonElement = fixture.nativeElement.querySelector('button');

    expect(boton.getAttribute('aria-label')).toBe('Mostrar contraseña');

    boton.click();
    fixture.detectChanges();
    expect(input.type).toBe('text');
    expect(boton.getAttribute('aria-label')).toBe('Ocultar contraseña');
    expect(boton.getAttribute('aria-pressed')).toBe('true');

    boton.click();
    fixture.detectChanges();
    expect(input.type).toBe('password');
  });
});
