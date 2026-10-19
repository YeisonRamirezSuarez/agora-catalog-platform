import {TestBed} from '@angular/core/testing';
import {AppComponent} from '@app/app.component';
import {commonTestProviders} from 'src/testing/test-providers';

describe('AppComponent', () => {

  it('se construye con sus dependencias', async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [...commonTestProviders]
    }).compileComponents();

    expect(TestBed.createComponent(AppComponent).componentInstance).toBeTruthy();
  });

});
