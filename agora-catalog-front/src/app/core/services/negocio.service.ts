import {inject, Injectable, signal} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {map, Observable, of, shareReplay, tap} from 'rxjs';

import {AuthService} from './auth.service';

/** Lo que el menú del panel muestra del negocio: nombre, logo y su link público. */
export interface Negocio {
  nombre: string;
  logoUrl: string | null;
  linkPublico: string;
}

/** Lo que se vende por separado; el catálogo básico lo tienen todos (GET /admin/modulos). */
export type Modulo = 'PEDIDO_WHATSAPP' | 'IMPORTAR_EXCEL' | 'COLECCIONES' | 'ESTADISTICAS' | 'VENTA_RUTA';

/**
 * El negocio del administrador con sesión y sus módulos, pedidos una sola vez y compartidos por
 * todas las pantallas del panel. Configuración lo actualiza al guardar, así el menú cambia sin recargar.
 */
@Injectable({providedIn: 'root'})
export class NegocioService {

  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private cargadoPara: number | null = null;
  private modulosPara: number | null = null;
  private modulos$: Observable<Set<Modulo>> | null = null;
  readonly negocio = signal<Negocio | null>(null);
  /** null mientras se piden: el menú no muestra los enlaces de módulos hasta saberlo. */
  readonly modulos = signal<Set<Modulo> | null>(null);

  /** Pide el negocio si cambió el usuario (otro admin pudo entrar en la misma pestaña). */
  cargar(): void {
    const u = this.auth.usuario();
    if (u?.rol !== 'ADMIN_EMPRESA') return;
    this.cargarModulos().subscribe({error: () => undefined});
    if (u.id === this.cargadoPara) return;
    this.cargadoPara = u.id;
    this.negocio.set(null);
    this.http.get<Negocio>('/admin/empresa').subscribe({
      next: c => this.actualizar(c),
      error: () => this.cargadoPara = null
    });
  }

  /** Los módulos encendidos del negocio; se piden una vez por usuario (los guards esperan este). */
  cargarModulos(): Observable<Set<Modulo>> {
    const u = this.auth.usuario();
    if (u?.rol !== 'ADMIN_EMPRESA') return of(new Set<Modulo>());
    if (u.id !== this.modulosPara || !this.modulos$) {
      this.modulosPara = u.id;
      this.modulos.set(null);
      this.modulos$ = this.http.get<Modulo[]>('/admin/modulos').pipe(
        map(lista => new Set(lista)),
        tap({
          next: s => this.modulos.set(s),
          error: () => { this.modulos$ = null; this.modulosPara = null; }
        }),
        shareReplay(1)
      );
    }
    return this.modulos$;
  }

  tiene(m: Modulo): boolean {
    return this.modulos()?.has(m) ?? false;
  }

  actualizar(c: Negocio): void {
    this.negocio.set({nombre: c.nombre, logoUrl: c.logoUrl, linkPublico: c.linkPublico});
  }
}
