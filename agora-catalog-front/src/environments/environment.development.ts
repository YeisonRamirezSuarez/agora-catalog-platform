/**
 * Configuración para desarrollo LOCAL.
 *
 * Angular reemplaza environment.ts por este archivo con la configuración `development`
 * (`npm start`), gracias al `fileReplacements` de angular.json.
 */
export const environment = {
  production: false,
  appVersion: '0.1.0',
  project_name: 'Agorá Catalog',

  // Backend local por el proxy de `ng serve` (proxy.conf.json -> localhost:9092): así el front
  // también funciona abierto desde otro equipo o un celular de la red, que no ven este localhost.
  api: '/api',
};
