import {defineConfig, devices} from '@playwright/test';

/**
 * Pruebas de pantalla (e2e/): el catálogo y el panel en un Chrome de verdad, con clics y archivos
 * reales, contra la app compilada servida por herramientas/servidor/serve_app.py y una API con los
 * datos de prueba (semilla). Cómo levantarlas: e2e/README.md. En CI las corre el job "Pantallas".
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 45_000,
  expect: {timeout: 8_000},
  // En serie: las pruebas del panel cambian datos del mismo negocio.
  workers: 1,
  fullyParallel: false,
  retries: process.env['CI'] ? 1 : 0,
  reporter: process.env['CI'] ? [['list'], ['html', {open: 'never'}]] : 'list',
  use: {
    baseURL: process.env['E2E_URL'] ?? 'http://127.0.0.1:4300',
    // En local, el Chrome instalado; en CI, el Chromium que baja Playwright.
    channel: process.env['CI'] ? undefined : 'chrome',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    {name: 'celular', use: {...devices['Pixel 7'], viewport: {width: 390, height: 844}}},
    {name: 'computador', use: {...devices['Desktop Chrome'], viewport: {width: 1280, height: 800}}},
  ],
});
