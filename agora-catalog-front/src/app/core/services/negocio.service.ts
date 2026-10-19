import {inject, Injectable, signal} from '@angular/core';
import {HttpClient} from '@angular/common/http';

import {AuthService} from './auth.service';

/** Lo que el menú del panel muestra del negocio: nombre, logo y su link público. */
export interface Negocio {
  nombre: string;
  logoUrl: string | null;
  linkPublico: string;
}

/**
 * El negocio del administrador con sesión, pedido una sola vez y compartido por todas las
 * pantallas del panel. Configuración lo actualiza al guardar, así el menú cambia sin recargar.
 */
@Injectable({providedIn: 'root'})
export class NegocioService {

  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private cargadoPara: number | null = null;
  readonly negocio = signal<Negocio | null>(null);

  /** Pide el negocio si cambió el usuario (otro admin pudo entrar en la misma pestaña). */
  cargar(): void {
    const u = this.auth.usuario();
    if (u?.rol !== 'ADMIN_EMPRESA' || u.id === this.cargadoPara) return;
    this.cargadoPara = u.id;
    this.negocio.set(null);
    this.http.get<Negocio>('/admin/empresa').subscribe({
      next: c => this.actualizar(c),
      error: () => this.cargadoPara = null
    });
  }

  actualizar(c: Negocio): void {
    this.negocio.set({nombre: c.nombre, logoUrl: c.logoUrl, linkPublico: c.linkPublico});
  }
}
