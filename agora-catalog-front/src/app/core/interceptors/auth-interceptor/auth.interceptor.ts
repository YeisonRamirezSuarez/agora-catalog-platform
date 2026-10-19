import {HttpErrorResponse, HttpInterceptorFn, HttpRequest} from '@angular/common/http';
import {inject} from '@angular/core';
import {Router} from '@angular/router';
import {catchError, switchMap, throwError} from 'rxjs';

import {AuthService} from '@app/core/services/auth.service';

/** Rutas de sesión: no llevan token ni se reintentan (un 401 ahí es la respuesta, no un token vencido). */
const SIN_TOKEN = ['/auth/login', '/auth/refresh', '/auth/logout'];

/**
 * Agrega el token de acceso y, si la API responde 401, renueva la sesión una vez y repite la
 * petición. Si la renovación falla, cierra la sesión y lleva al login.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (SIN_TOKEN.some(path => request.url.endsWith(path))) {
    return next(request);
  }
  const auth = inject(AuthService);
  const router = inject(Router);

  return next(withToken(request, auth.accessToken)).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401 || !auth.accessToken) {
        return throwError(() => error);
      }
      return auth.refresh().pipe(
        catchError(refreshError => {
          auth.clear();
          void router.navigateByUrl('/login');
          return throwError(() => refreshError);
        }),
        switchMap(sesion => next(withToken(request, sesion.accessToken)))
      );
    })
  );
};

function withToken(request: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  return token ? request.clone({setHeaders: {Authorization: `Bearer ${token}`}}) : request;
}
