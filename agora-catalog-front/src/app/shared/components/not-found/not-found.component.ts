import {Component} from '@angular/core';

/** Ruta que no existe: marca de la plataforma y un mensaje corto. */
@Component({
  selector: 'app-not-found',
  template: `
    <main class="grid min-h-dvh place-items-center bg-cream px-4 text-center">
      <div class="flex max-w-sm flex-col items-center gap-6">
        <img src="assets/images/brand/logo.svg" alt="Agorá Catalog" class="h-12 w-auto">
        <div>
          <h1 class="font-display text-3xl font-semibold text-ink">Esta página no existe</h1>
          <p class="mt-2 text-cream-muted">Revisa el link que te compartieron o pídelo de nuevo al negocio.</p>
        </div>
      </div>
    </main>
  `
})
export class NotFoundComponent {
}
