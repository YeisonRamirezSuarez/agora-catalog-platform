import {expect, test} from '@playwright/test';

import {CATALOGO, HASH, sinDesbordeHorizontal, sinWhatsapp} from './ayudas';

/** El catálogo público como lo usa un cliente. */
test.describe('catálogo público', () => {
  test.beforeEach(async ({page}) => {
    await sinWhatsapp(page);
    await page.goto(CATALOGO + '/catalogo');
    await page.evaluate(() => { localStorage.clear(); sessionStorage.clear(); });
  });

  test('nada se sale de los lados en inicio, catálogo y contacto', async ({page}) => {
    for (const ruta of ['', '/catalogo', '/contacto']) {
      await page.goto(CATALOGO + ruta);
      await expect(page.locator('.cat-root .store')).toBeVisible();
      await sinDesbordeHorizontal(page);
    }
  });

  test('el pedido sale por WhatsApp con el número elegido y el mensaje armado', async ({page}) => {
    await page.goto(CATALOGO + '/catalogo');
    await page.locator('.add').first().click();
    await page.locator('.cart-btn').click();
    await page.getByRole('button', {name: 'Enviar pedido por WhatsApp'}).click();

    // Javier tiene dos números: la hoja para elegir, con sus botones dentro de la tarjeta.
    const hoja = page.getByRole('dialog', {name: 'Elegir número de WhatsApp'});
    await expect(hoja.getByRole('heading', {name: '¿A qué número le escribes?'})).toBeVisible();
    const numeros = hoja.locator('.wa-list a');
    await expect(numeros).toHaveCount(2);
    const caja = (await hoja.boundingBox())!;
    for (const b of [await numeros.nth(0).boundingBox(), await numeros.nth(1).boundingBox()]) {
      expect(b!.y + b!.height, 'el botón queda dentro de la hoja').toBeLessThanOrEqual(caja.y + caja.height);
    }
    const href = decodeURIComponent((await numeros.first().getAttribute('href'))!);
    expect(href).toContain('https://wa.me/573163218276?text=');
    expect(href).toContain('Hola, quiero hacer este pedido');

    // El enlace abre WhatsApp (una pestaña nueva) y cuenta el pedido para las estadísticas.
    const evento = page.waitForRequest(r => r.url().endsWith(`/api/public/catalogos/${HASH}/eventos`) && r.postData()!.includes('PEDIDO'));
    const pestana = page.context().waitForEvent('page');
    await numeros.first().click();
    await evento;
    await (await pestana).close();
  });

  test('mover el filtro de precio no sube la página', async ({page, isMobile}) => {
    test.skip(isMobile, 'en el celular los filtros van en una hoja');
    await page.goto(CATALOGO + '/catalogo');
    const rango = page.getByLabel('Precio máximo');
    await page.evaluate(() => window.scrollTo(0, 500));
    await page.waitForTimeout(300);
    const antes = await page.evaluate(() => window.scrollY);
    const b = (await rango.boundingBox())!;
    await page.mouse.move(b.x + b.width - 6, b.y + b.height / 2);
    await page.mouse.down();
    await page.mouse.move(b.x + b.width / 2, b.y + b.height / 2, {steps: 8});
    await page.mouse.up();
    await expect(page).toHaveURL(/max=/);
    expect(Math.abs(await page.evaluate(() => window.scrollY) - antes)).toBeLessThan(40);
  });

  test('el link de un producto abre su hoja y se puede compartir', async ({page, request}) => {
    const catalogo = await (await request.get(`/api/public/catalogos/${HASH}`)).json();
    const producto = catalogo.productos[0];
    await page.goto(`${CATALOGO}/catalogo?p=${producto.id}`);
    const hoja = page.getByRole('dialog', {name: producto.nombre});
    await expect(hoja.getByRole('heading', {name: producto.nombre})).toBeVisible();
    await expect(hoja.getByRole('button', {name: 'Compartir este producto'})).toBeVisible();

    // La vista previa de WhatsApp de ese link (la arma el backend).
    const html = await (await request.get(`${CATALOGO}/catalogo?p=${producto.id}`)).text();
    expect(html).toContain(`<meta property="og:title" content="${producto.nombre} · Salsamentaria Javier">`);
  });

  test('la visita cuenta para las estadísticas una sola vez por sesión', async ({page}) => {
    const visitas: string[] = [];
    page.on('request', r => { if (r.url().endsWith('/eventos') && r.postData()?.includes('VISITA')) visitas.push(r.url()); });
    await page.goto(CATALOGO);
    await expect(page.locator('.cat-root .store')).toBeVisible();
    await page.reload();
    await expect(page.locator('.cat-root .store')).toBeVisible();
    await page.waitForTimeout(500);
    expect(visitas).toHaveLength(1);
  });
});
