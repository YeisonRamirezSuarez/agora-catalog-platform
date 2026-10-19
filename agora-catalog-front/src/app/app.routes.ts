import {Routes} from '@angular/router';

export const routes: Routes = [
  {
    path: '**',
    loadComponent: () => import('@app/shared/components/not-found/not-found.component')
      .then(c => c.NotFoundComponent)
  }
];
