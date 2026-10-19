import {HttpInterceptorFn} from '@angular/common/http';

import {environment} from '@env/environment';

/** Antepone la URL de la API: los servicios piden `/public/...`, no `/api/public/...`. */
export const apiPrefixInterceptor: HttpInterceptorFn = (request, next) =>
  next(request.clone({url: environment.api + request.url}));
