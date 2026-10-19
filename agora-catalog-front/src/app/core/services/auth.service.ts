import {computed, inject, Injectable, signal} from '@angular/core';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {Router} from '@angular/router';
import {finalize, Observable, shareReplay, tap} from 'rxjs';

import {Sesion, SesionUsuario} from '@app/core/interfaces/sesion.interface';

const STORAGE_KEY = 'ac-sesion';

/** Mensaje de error de la API para mostrar tal cual, con uno genérico si no llegó. */
export function apiMessage(error: unknown, fallback = 'No pudimos completar la acción. Intenta de nuevo.'): string {
  return error instanceof HttpErrorResponse && typeof error.error?.message === 'string' ? error.error.message : fallback;
}

/**
 * Sesión del panel: tokens y usuario en `localStorage`, para que un recargo no cierre la sesión.
 * Sin almacenamiento (modo privado estricto) la sesión dura mientras la página esté abierta.
 */
@Injectable({providedIn: 'root'})
export class AuthService {

  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly sesion = signal<Sesion | null>(read());
  private refreshing$: Observable<Sesion> | null = null;

  readonly usuario = computed<SesionUsuario | null>(() => this.sesion()?.usuario ?? null);

  get accessToken(): string | null {
    return this.sesion()?.accessToken ?? null;
  }

  login(email: string, password: string): Observable<Sesion> {
    return this.http.post<Sesion>('/auth/login', {email, password}).pipe(tap(s => this.save(s)));
  }

  /** Una sola renovación a la vez: varias peticiones con 401 esperan la misma. */
  refresh(): Observable<Sesion> {
    const refreshToken = this.sesion()?.refreshToken;
    if (!this.refreshing$) {
      this.refreshing$ = this.http.post<Sesion>('/auth/refresh', {refreshToken}).pipe(
        tap(s => this.save(s)),
        finalize(() => this.refreshing$ = null),
        shareReplay(1)
      );
    }
    return this.refreshing$;
  }

  changePassword(actual: string, nueva: string): Observable<void> {
    return this.http.put<void>('/auth/password', {actual, nueva}).pipe(tap(() => {
      const actualSesion = this.sesion();
      if (actualSesion) this.save({...actualSesion, usuario: {...actualSesion.usuario, debeCambiarPassword: false}});
    }));
  }

  logout(): void {
    const refreshToken = this.sesion()?.refreshToken;
    if (refreshToken) {
      // Si falla, el token vence solo: la sesión local se cierra igual.
      this.http.post('/auth/logout', {refreshToken}).subscribe({error: () => undefined});
    }
    this.clear();
    void this.router.navigateByUrl('/login');
  }

  clear(): void {
    this.sesion.set(null);
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      // Sin almacenamiento no hay nada que borrar.
    }
  }

  /** A dónde va cada quien al entrar. */
  home(usuario: SesionUsuario | null = this.usuario()): string {
    if (!usuario) return '/login';
    if (usuario.debeCambiarPassword) return '/cambiar-contrasena';
    return usuario.rol === 'SUPER_ADMIN' ? '/superadmin' : '/admin';
  }

  private save(sesion: Sesion): void {
    this.sesion.set(sesion);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(sesion));
    } catch {
      // Sin almacenamiento la sesión vive solo en memoria.
    }
  }
}

function read(): Sesion | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) as Sesion : null;
  } catch {
    return null;
  }
}
