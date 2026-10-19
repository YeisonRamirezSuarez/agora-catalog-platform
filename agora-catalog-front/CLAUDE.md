# CLAUDE.md — frontend

Angular 21 standalone · signals · Tailwind 3 · Vitest · CSR (sin SSR).
Las reglas que cruzan front y back están en el `CLAUDE.md` de la raíz.

## Estructura

```
src/
├── app/
│   ├── core/          guards, interceptors, services e interfaces de toda la app
│   ├── shared/        componentes, layouts y utilidades reutilizables
│   └── modules/       una carpeta por área: catalogo (público), admin, superadmin, auth
├── assets/images/brand/   marca Agorá Catalog (copiada de docs/prototipo/marca)
├── environments/      environment.ts (desplegado) y environment.development.ts (local)
└── testing/           test-setup.ts (huecos de jsdom) y test-providers.ts
public/                favicon, íconos de app y site.webmanifest
```

Alias de importación: `@app/*` → `src/app/*`, `@env/*` → `src/environments/*`.

## Convenciones

- Componentes standalone, `ChangeDetection` por defecto con zone.js y estado con signals.
- Rutas con `loadComponent` / `loadChildren` (carga diferida).
- Los servicios piden rutas sin `/api` (`/public/catalogos/...`): `apiPrefixInterceptor` lo antepone.
- Estilos con clases de Tailwind. Los tokens salen de `DESIGN.md` y viven en `tailwind.config.js`;
  `brand` y `accent` son las variables CSS `--brand` y `--accent` de cada negocio.
- Tipografías: Playfair Display (`font-display`), Ms Madi (`font-hand`), Onest (`font-sans`).
- Texto para el usuario en español neutro con tuteo. Precios en COP sin decimales: `$12.500`.
- Mobile-first: el catálogo se usa sobre todo en celulares de gama media con datos móviles.

## Verificación

```bash
npm run build                 # build de producción
npx ng test --watch=false     # Vitest
npm start                     # http://localhost:4251, con proxy de /api a localhost:9092
```

Un spec nuevo usa `commonTestProviders` de `src/testing/test-providers.ts`.
