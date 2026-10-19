import {TestBed} from '@angular/core/testing';
import {HttpClient, provideHttpClient, withInterceptors} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';

import {environment} from '@env/environment';
import {apiPrefixInterceptor} from './api-prefix.interceptor';

describe('apiPrefixInterceptor', () => {

  it('antepone la URL de la API', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiPrefixInterceptor])),
        provideHttpClientTesting()
      ]
    });
    const http = TestBed.inject(HttpClient);
    const backend = TestBed.inject(HttpTestingController);

    http.get('/public/catalogos/x7k2m9ab').subscribe();

    backend.expectOne(environment.api + '/public/catalogos/x7k2m9ab').flush(null);
    backend.verify();
  });

});
