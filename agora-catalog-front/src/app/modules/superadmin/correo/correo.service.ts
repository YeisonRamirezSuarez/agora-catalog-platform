import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {map, Observable} from 'rxjs';

export type Seguridad = 'STARTTLS' | 'SSL' | 'NINGUNA';

export interface ProveedorCorreo {
  host: string;
  puerto: number;
  usuario: string | null;
  passwordGuardada: boolean;
  remitenteEmail: string;
  remitenteNombre: string;
  seguridad: Seguridad;
  activo: boolean;
  updatedAt: string;
}

/** Lo que se envía al guardar. `password` vacía conserva la guardada. */
export interface ProveedorCorreoForm {
  host: string;
  puerto: number;
  usuario: string;
  password: string;
  remitenteEmail: string;
  remitenteNombre: string;
  seguridad: Seguridad;
  activo: boolean;
}

export interface ResultadoEnvio {
  enviado: boolean;
  error: string | null;
}

export interface CorreoEnvio {
  id: number;
  empresaId: number | null;
  destinatario: string;
  asunto: string;
  tipo: string;
  enviado: boolean;
  error: string | null;
  createdAt: string;
}

@Injectable({providedIn: 'root'})
export class CorreoService {

  private readonly http = inject(HttpClient);

  /** null mientras no se haya configurado (la API responde 204). */
  get(): Observable<ProveedorCorreo | null> {
    return this.http.get<ProveedorCorreo | null>('/superadmin/correo').pipe(map(p => p ?? null));
  }

  save(form: ProveedorCorreoForm): Observable<ProveedorCorreo> {
    return this.http.put<ProveedorCorreo>('/superadmin/correo', form);
  }

  probar(destinatario: string): Observable<ResultadoEnvio> {
    return this.http.post<ResultadoEnvio>('/superadmin/correo/prueba', {destinatario});
  }

  envios(): Observable<CorreoEnvio[]> {
    return this.http.get<CorreoEnvio[]>('/superadmin/correo/envios');
  }
}
