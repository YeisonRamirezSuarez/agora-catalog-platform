import {expect, Page} from '@playwright/test';
import path from 'node:path';

/** Negocio de la semilla (db/seed/R__semilla_local.sql) y su administrador de prueba. */
export const HASH = 'x7k2m9ab';
export const CATALOGO = '/c/salsamentaria-javier-x7k2m9ab';
export const ADMIN = {email: 'javier@agora.local', password: 'Agora123*'};
export const FOTO = path.join(__dirname, 'fixtures', 'foto.jpg');

/** WhatsApp nunca se abre de verdad: se responde una página vacía y la prueba mira el link. */
export async function sinWhatsapp(page: Page): Promise<void> {
  for (const url of ['https://wa.me/**', 'https://api.whatsapp.com/**']) {
    await page.context().route(url, r => r.fulfill({body: 'whatsapp', contentType: 'text/plain'}));
  }
}

export async function entrarAlPanel(page: Page, cuenta = ADMIN): Promise<void> {
  await page.goto('/login');
  await page.evaluate(() => localStorage.clear());
  await page.goto('/login');
  await page.getByLabel('Correo').fill(cuenta.email);
  await page.getByLabel('Contraseña', {exact: true}).fill(cuenta.password);
  await page.getByRole('button', {name: 'Ingresar'}).click();
  await expect(page).not.toHaveURL(/\/login$/);
}

/** Nada se sale de los lados de la pantalla. */
export async function sinDesbordeHorizontal(page: Page): Promise<void> {
  const [ancho, vista] = await page.evaluate(() => [document.documentElement.scrollWidth, window.innerWidth]);
  expect(ancho, 'ancho de la página').toBeLessThanOrEqual(vista);
}

/** Abre el menú del panel (en celular es una fila arriba; en computador, el lateral). */
export async function irEnElPanel(page: Page, opcion: string): Promise<void> {
  await page.getByRole('navigation', {name: 'Menú'}).filter({visible: true}).getByRole('link', {name: opcion, exact: true}).click();
}
