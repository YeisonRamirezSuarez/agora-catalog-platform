import {EnvironmentProviders, Provider} from '@angular/core';
import {provideRouter} from '@angular/router';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';

/**
 * Providers que casi todo componente necesita para construirse en un test. Un spec que necesite
 * algo más agrega su provider **después** de este arreglo: el último gana.
 *
 * `provideHttpClientTesting` deja las peticiones en un backend falso: ningún test sale a la red
 * ni depende de que el backend local esté arriba.
 */
export const commonTestProviders: (Provider | EnvironmentProviders)[] = [
  provideHttpClient(),
  provideHttpClientTesting(),
  provideRouter([]),
];
