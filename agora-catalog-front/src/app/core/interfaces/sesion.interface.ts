export type Rol = 'SUPER_ADMIN' | 'ADMIN_EMPRESA';

export interface SesionUsuario {
  id: number;
  nombre: string;
  email: string;
  rol: Rol;
  empresaId: number | null;
  empresaNombre: string | null;
  debeCambiarPassword: boolean;
}

/** Respuesta de /auth/login y /auth/refresh; también es lo que se guarda en el navegador. */
export interface Sesion {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  usuario: SesionUsuario;
}

/** Cuerpo de todo error de la API. */
export interface ApiError {
  error: string;
  message: string;
}
