import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';

export interface Administrador {
  id: number;
  nombre: string;
  email: string;
  activo: boolean;
  debeCambiarPassword: boolean;
}

export interface Empresa {
  id: number;
  nombre: string;
  slug: string;
  publicHash: string;
  linkPublico: string;
  activa: boolean;
  productos: number;
  administradores: Administrador[];
}

export interface AdminCreado {
  id: number;
  nombre: string;
  email: string;
  passwordTemporal: string;
  correoEnviado: boolean;
  correoError: string | null;
}

/** Un módulo de la plataforma y si la empresa lo tiene encendido. */
export interface ModuloEmpresa {
  modulo: string;
  nombre: string;
  descripcion: string;
  activo: boolean;
}

@Injectable({providedIn: 'root'})
export class EmpresasService {

  private readonly http = inject(HttpClient);

  list(): Observable<Empresa[]> {
    return this.http.get<Empresa[]>('/superadmin/empresas');
  }

  create(nombre: string, slug: string): Observable<Empresa> {
    return this.http.post<Empresa>('/superadmin/empresas', {nombre, slug});
  }

  update(id: number, nombre: string, slug: string): Observable<Empresa> {
    return this.http.put<Empresa>(`/superadmin/empresas/${id}`, {nombre, slug});
  }

  setActiva(id: number, activo: boolean): Observable<void> {
    return this.http.patch<void>(`/superadmin/empresas/${id}/activa`, {activo});
  }

  createAdmin(empresaId: number, nombre: string, email: string, enviarCorreo: boolean): Observable<AdminCreado> {
    return this.http.post<AdminCreado>(`/superadmin/empresas/${empresaId}/usuarios`, {nombre, email, enviarCorreo});
  }

  /** Contraseña temporal nueva para un administrador que olvidó la suya. */
  restablecerPassword(id: number, enviarCorreo: boolean): Observable<AdminCreado> {
    return this.http.post<AdminCreado>(`/superadmin/usuarios/${id}/password`, {enviarCorreo});
  }

  modulos(empresaId: number): Observable<ModuloEmpresa[]> {
    return this.http.get<ModuloEmpresa[]>(`/superadmin/empresas/${empresaId}/modulos`);
  }

  setModulo(empresaId: number, modulo: string, activo: boolean): Observable<ModuloEmpresa[]> {
    return this.http.patch<ModuloEmpresa[]>(`/superadmin/empresas/${empresaId}/modulos/${modulo}`, {activo});
  }

  setUsuarioActivo(id: number, activo: boolean): Observable<void> {
    return this.http.patch<void>(`/superadmin/usuarios/${id}/activo`, {activo});
  }
}
