/**
 * Configuración del ambiente DESPLEGADO. Es el archivo que usa `npm run build`.
 *
 * No pongas aquí una URL de localhost: el navegador de cada visitante intentaría conectarse
 * a su propia máquina. Para desarrollo local está `environment.development.ts`, que Angular
 * sustituye automáticamente al correr `npm start` (ver `fileReplacements` en angular.json).
 */
export const environment = {
  production: true,
  appVersion: '0.1.0',
  project_name: 'Agorá Catalog',

  // Ruta relativa: front y back se publican bajo el mismo dominio y Caddy reenvía /api/ al
  // backend. Al ser el mismo origen, no interviene CORS.
  api: '/api',
};
