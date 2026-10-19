import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {RouterLink} from '@angular/router';
import {switchMap} from 'rxjs';

import {apiMessage} from '@app/core/services/auth.service';
import {PanelShellComponent} from '@app/shared/layouts/panel-shell/panel-shell.component';
import {CorreoService} from '../correo/correo.service';
import {SUPERADMIN_LINKS} from '../superadmin-links';
import {AdminCreado, Administrador, Empresa, EmpresasService} from './empresas.service';

/** Lo que se muestra una sola vez después de crear un administrador. */
interface Credenciales extends AdminCreado {
  titulo: string;
}

@Component({
  selector: 'app-empresas',
  imports: [PanelShellComponent, ReactiveFormsModule, RouterLink],
  template: `
    <app-panel-shell subtitle="Super admin" [links]="links">
      <header class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 class="page-title">Empresas</h1>
          <p class="mt-1 text-muted">{{ resumen() }}</p>
        </div>
        @if (!formAbierto()) {
          <button type="button" class="btn btn-dark" (click)="abrirNueva()">+ Nueva empresa</button>
        }
      </header>

      @if (error()) {
        <p class="alert-error mt-6" role="alert">{{ error() }}</p>
      }

      @if (credenciales(); as c) {
        <section class="card mt-6 grid gap-3 border-ok/30 p-5" aria-live="polite">
          <h2 class="text-lg font-semibold">{{ c.titulo }}</h2>
          @if (c.correoEnviado) {
            <p class="alert-ok">Le enviamos estos datos a {{ c.email }}.</p>
          } @else if (c.correoError) {
            <p class="alert-error">No salió el correo: {{ c.correoError }} Entrégale los datos tú mismo.</p>
          }
          <p class="text-sm text-muted">
            La contraseña temporal se muestra solo esta vez y se le pedirá cambiarla al entrar.
          </p>
          <dl class="grid gap-2 rounded-[10px] bg-wash p-4 text-sm sm:grid-cols-[140px_1fr]">
            <dt class="text-muted">Correo</dt><dd class="font-semibold">{{ c.email }}</dd>
            <dt class="text-muted">Contraseña temporal</dt><dd class="font-mono text-base font-semibold">{{ c.passwordTemporal }}</dd>
          </dl>
          <div class="flex flex-wrap gap-2">
            <button type="button" class="btn" (click)="copiar(c)">{{ copiado() ? 'Copiado' : 'Copiar datos' }}</button>
            <button type="button" class="btn-link" (click)="credenciales.set(null)">Cerrar</button>
          </div>
        </section>
      }

      @if (editando(); as e) {
        <form [formGroup]="editForm" (ngSubmit)="guardarEdicion(e)" novalidate class="card mt-6 grid gap-5 p-5 sm:p-6">
          <h2 class="text-lg font-semibold">Editar {{ e.nombre }}</h2>
          <div class="grid gap-4 sm:grid-cols-2">
            <div class="field">
              <label for="e-nombre">Nombre del negocio</label>
              <input class="input" id="e-nombre" formControlName="nombre" maxlength="120">
            </div>
            <div class="field">
              <label for="e-slug">Slug del link</label>
              <input class="input" id="e-slug" formControlName="slug" maxlength="80" aria-describedby="h-eslug">
              <p class="hint" id="h-eslug">El link sigue funcionando: lo que lo identifica es el código del final ({{ e.publicHash }}).</p>
            </div>
          </div>
          <div class="flex flex-wrap gap-2">
            <button class="btn btn-dark" [disabled]="guardando()">{{ guardando() ? 'Guardando…' : 'Guardar' }}</button>
            <button type="button" class="btn" (click)="editando.set(null)">Cancelar</button>
          </div>
        </form>
      }

      @if (formAbierto()) {
        <form [formGroup]="form" (ngSubmit)="guardar()" novalidate class="card mt-6 grid gap-5 p-5 sm:p-6">
          <h2 class="text-lg font-semibold">{{ empresaParaAdmin() ? 'Administrador de ' + empresaParaAdmin()!.nombre : 'Nueva empresa' }}</h2>
          @if (!empresaParaAdmin()) {
            <div class="grid gap-4 sm:grid-cols-2">
              <div class="field">
                <label for="f-nombre">Nombre del negocio</label>
                <input class="input" id="f-nombre" formControlName="nombre" maxlength="120" placeholder="Panadería La Espiga">
              </div>
              <div class="field">
                <label for="f-slug">Slug del link <span class="font-normal text-muted">(opcional)</span></label>
                <input class="input" id="f-slug" formControlName="slug" maxlength="80" placeholder="panaderia-la-espiga"
                       aria-describedby="h-slug">
                <p class="hint" id="h-slug">Si lo dejas vacío, sale del nombre. Se puede cambiar después sin romper el link.</p>
              </div>
            </div>
          }
          <div class="grid gap-4 sm:grid-cols-2">
            <div class="field">
              <label for="f-admin">Nombre del administrador</label>
              <input class="input" id="f-admin" formControlName="adminNombre" maxlength="120" autocomplete="off">
            </div>
            <div class="field">
              <label for="f-email">Correo del administrador</label>
              <input class="input" id="f-email" type="email" formControlName="adminEmail" maxlength="160" autocomplete="off">
            </div>
          </div>
          <div>
            <label class="inline-flex items-center gap-3 font-semibold" [class.opacity-50]="!correoActivo()">
              <input type="checkbox" class="size-5 accent-[#1C1814]" formControlName="enviarCorreo">
              Enviarle su usuario y contraseña por correo
            </label>
            @if (!correoActivo()) {
              <p class="hint mt-1">Para enviarlo, configura primero el <a routerLink="/superadmin/correo" class="font-semibold text-ink underline">proveedor de correo</a>.</p>
            }
          </div>
          <div class="flex flex-wrap gap-2">
            <button class="btn btn-dark" [disabled]="guardando()">{{ guardando() ? 'Guardando…' : 'Crear' }}</button>
            <button type="button" class="btn" (click)="cerrarForm()">Cancelar</button>
          </div>
        </form>
      }

      <div class="card mt-6 overflow-x-auto">
        <table class="table min-w-[760px]">
          <thead>
            <tr><th>Empresa</th><th>Link público</th><th class="text-right">Productos</th><th>Administradores</th><th>Activa</th></tr>
          </thead>
          <tbody>
            @for (e of empresas(); track e.id) {
              <tr>
                <td>
                  <span class="font-semibold">{{ e.nombre }}</span>
                  <button type="button" class="btn-link ml-2 text-xs" (click)="abrirEdicion(e)">Editar</button>
                </td>
                <td><a class="text-muted underline-offset-2 hover:text-ink" [href]="e.linkPublico" target="_blank" rel="noopener">{{ e.linkPublico }}</a></td>
                <td class="text-right tabular-nums">{{ e.productos }}</td>
                <td>
                  <ul class="grid gap-1.5">
                    @for (a of e.administradores; track a.id) {
                      <li class="flex flex-wrap items-center gap-2">
                        <span [class.text-faint]="!a.activo" [class.line-through]="!a.activo">{{ a.email }}</span>
                        @if (a.debeCambiarPassword && a.activo) {
                          <span class="badge bg-warn-soft text-warn-ink">Contraseña temporal</span>
                        }
                        <button type="button" class="btn-link text-xs" (click)="toggleAdmin(a)">
                          {{ a.activo ? 'Desactivar' : 'Activar' }}
                        </button>
                        @if (porRestablecer() === a.id) {
                          <span class="inline-flex flex-wrap items-center gap-2 text-xs">
                            ¿Cambiarle la contraseña? Se cierran sus sesiones.
                            <button type="button" class="btn-link text-xs text-danger" (click)="restablecer(a, e)">Sí, cambiar</button>
                            <button type="button" class="btn-link text-xs" (click)="porRestablecer.set(null)">No</button>
                          </span>
                        } @else if (a.activo) {
                          <button type="button" class="btn-link text-xs" (click)="porRestablecer.set(a.id)">Nueva contraseña</button>
                        }
                      </li>
                    } @empty {
                      <li class="text-faint">Sin administrador</li>
                    }
                  </ul>
                  <button type="button" class="btn-link mt-1.5 text-xs" (click)="abrirAdmin(e)">+ Agregar administrador</button>
                </td>
                <td>
                  <input type="checkbox" class="switch" role="switch" [checked]="e.activa"
                         [attr.aria-label]="e.nombre + ' activa'" (change)="toggleEmpresa(e, $event)">
                </td>
              </tr>
            } @empty {
              <tr><td colspan="5" class="py-10 text-center text-muted">{{ cargando() ? 'Cargando…' : 'Todavía no hay empresas.' }}</td></tr>
            }
          </tbody>
        </table>
      </div>
      <p class="hint mt-3">
        Al desactivar una empresa, su catálogo deja de estar disponible y sus administradores pierden el acceso en menos de 15 minutos.
      </p>
    </app-panel-shell>
  `
})
export class EmpresasComponent implements OnInit {

  private readonly api = inject(EmpresasService);
  private readonly correo = inject(CorreoService);
  readonly links = SUPERADMIN_LINKS;
  readonly correoActivo = signal(false);

  readonly empresas = signal<Empresa[]>([]);
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly formAbierto = signal(false);
  readonly empresaParaAdmin = signal<Empresa | null>(null);
  readonly guardando = signal(false);
  readonly credenciales = signal<Credenciales | null>(null);
  readonly copiado = signal(false);
  readonly editando = signal<Empresa | null>(null);
  readonly porRestablecer = signal<number | null>(null);

  readonly resumen = computed(() => {
    const total = this.empresas().length;
    const activas = this.empresas().filter(e => e.activa).length;
    return `${total} ${total === 1 ? 'empresa' : 'empresas'}, ${activas} ${activas === 1 ? 'activa' : 'activas'}.`;
  });

  readonly form = inject(FormBuilder).nonNullable.group({
    nombre: [''],
    slug: [''],
    adminNombre: ['', Validators.required],
    adminEmail: ['', [Validators.required, Validators.email]],
    enviarCorreo: [{value: false, disabled: true}]
  });

  readonly editForm = inject(FormBuilder).nonNullable.group({
    nombre: ['', Validators.required],
    slug: ['']
  });

  ngOnInit(): void {
    this.cargar();
    this.correo.get().subscribe({
      next: p => {
        this.correoActivo.set(!!p?.activo);
        if (p?.activo) this.form.controls.enviarCorreo.enable();
      },
      error: () => undefined
    });
  }

  abrirNueva(): void {
    this.editando.set(null);
    this.empresaParaAdmin.set(null);
    this.resetForm();
  }

  abrirAdmin(empresa: Empresa): void {
    this.editando.set(null);
    this.empresaParaAdmin.set(empresa);
    this.resetForm();
  }

  /** Con el correo activo, enviar los datos viene marcado: es lo que se hace casi siempre. */
  private resetForm(): void {
    this.form.reset({enviarCorreo: this.correoActivo()});
    this.formAbierto.set(true);
  }

  cerrarForm(): void {
    this.formAbierto.set(false);
    this.empresaParaAdmin.set(null);
  }

  guardar(): void {
    const {nombre, slug, adminNombre, adminEmail, enviarCorreo} = this.form.getRawValue();
    const enviar = this.correoActivo() && enviarCorreo;
    const destino = this.empresaParaAdmin();
    if (!destino && !nombre.trim()) {
      this.error.set('Escribe el nombre del negocio');
      return;
    }
    if (this.form.invalid) {
      this.error.set(this.form.controls.adminEmail.hasError('email')
        ? 'El correo del administrador no es válido'
        : 'Escribe el nombre y el correo del administrador');
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    // Primero la empresa (si es nueva) y después su administrador.
    const empresa$ = destino
      ? this.api.createAdmin(destino.id, adminNombre, adminEmail, enviar)
      : this.api.create(nombre, slug.trim()).pipe(
          switchMap(e => this.api.createAdmin(e.id, adminNombre, adminEmail, enviar)));
    empresa$.subscribe({
      next: admin => {
        this.credenciales.set({...admin, titulo: `Administrador de ${destino?.nombre ?? nombre.trim()} creado`});
        this.copiado.set(false);
        this.guardando.set(false);
        this.cerrarForm();
        this.cargar();
      },
      error: err => {
        this.error.set(apiMessage(err));
        this.guardando.set(false);
        // Si la empresa alcanzó a crearse, queda en la lista para agregarle el administrador.
        this.cargar();
      }
    });
  }

  abrirEdicion(e: Empresa): void {
    this.cerrarForm();
    this.editForm.reset({nombre: e.nombre, slug: e.slug});
    this.editando.set(e);
  }

  guardarEdicion(e: Empresa): void {
    const {nombre, slug} = this.editForm.getRawValue();
    if (!nombre.trim()) {
      this.error.set('Escribe el nombre del negocio');
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    this.api.update(e.id, nombre.trim(), slug.trim()).subscribe({
      next: () => { this.guardando.set(false); this.editando.set(null); this.cargar(); },
      error: err => { this.guardando.set(false); this.error.set(apiMessage(err)); }
    });
  }

  /** Con el correo activo, la contraseña nueva le llega por correo; si no, se muestra para entregarla. */
  restablecer(admin: Administrador, empresa: Empresa): void {
    this.porRestablecer.set(null);
    this.error.set('');
    this.api.restablecerPassword(admin.id, this.correoActivo()).subscribe({
      next: r => {
        this.credenciales.set({...r, titulo: `Contraseña nueva para ${r.email} (${empresa.nombre})`});
        this.copiado.set(false);
        this.cargar();
      },
      error: err => this.error.set(apiMessage(err))
    });
  }

  toggleEmpresa(empresa: Empresa, event: Event): void {
    const input = event.target as HTMLInputElement;
    const activa = input.checked;
    this.api.setActiva(empresa.id, activa).subscribe({
      next: () => this.empresas.update(list => list.map(e => e.id === empresa.id ? {...e, activa} : e)),
      error: err => {
        input.checked = !activa;
        this.error.set(apiMessage(err));
      }
    });
  }

  toggleAdmin(admin: Administrador): void {
    this.api.setUsuarioActivo(admin.id, !admin.activo).subscribe({
      next: () => this.cargar(),
      error: err => this.error.set(apiMessage(err))
    });
  }

  copiar(c: Credenciales): void {
    const texto = `Agorá Catalog\nCorreo: ${c.email}\nContraseña temporal: ${c.passwordTemporal}`;
    navigator.clipboard?.writeText(texto).then(() => this.copiado.set(true), () => this.copiado.set(false));
  }

  private cargar(): void {
    this.api.list().subscribe({
      next: list => {
        this.empresas.set(list);
        this.cargando.set(false);
      },
      error: err => {
        this.error.set(apiMessage(err));
        this.cargando.set(false);
      }
    });
  }
}
