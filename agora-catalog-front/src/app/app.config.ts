import {ApplicationConfig, provideZoneChangeDetection} from '@angular/core';
import {provideRouter, withComponentInputBinding} from '@angular/router';
import {provideHttpClient, withInterceptors} from '@angular/common/http';

import {routes} from './app.routes';
import {apiPrefixInterceptor} from '@app/core/interceptors/api-prefix-interceptor/api-prefix.interceptor';
import {authInterceptor} from '@app/core/interceptors/auth-interceptor/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({eventCoalescing: true}),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([apiPrefixInterceptor, authInterceptor])),
  ]
};
