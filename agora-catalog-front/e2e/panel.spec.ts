import {expect, test} from '@playwright/test';

import {CATALOGO, entrarAlPanel, FOTO, irEnElPanel, sinDesbordeHorizontal} from './ayudas';

/** El panel del negocio con el administrador de prueba de la semilla. */
test.describe('panel del negocio', () => {
  test.beforeEach(async ({page}) => {
    await entrarAlPanel(page);
    await expect(page).toHaveURL(/\/admin$/);
  });

  test('inicio: contadores, QR y estadísticas; menú usable', async ({page, isMobile}) => {
    await expect(page.getByRole('img', {name: 'Código QR del catálogo'})).toBeVisible();
    await expect(page.getByRole('heading', {name: /Tu catálogo en los últimos 30 días/})).toBeVisible();
    if (isMobile) await expect(page.getByRole('navigation', {name: 'Menú'}).filter({visible: true}).getByRole('link')).toHaveCount(6);
    await sinDesbordeHorizontal(page);
  });

  test('franja: "Elegir foto" abre el selector, guarda y se ve en el inicio del catálogo', async ({page}) => {
    await irEnElPanel(page, 'Categorías');
    await page.locator('li').filter({hasText: 'Franja en el inicio'}).getByRole('button', {name: 'Franja'}).click();
    const hoja = page.getByRole('dialog', {name: /^Franja de /});
    // El selector de archivos se abre con un clic de verdad en el botón (el label del input).
    const selector = page.waitForEvent('filechooser');
    await hoja.getByText(/Elegir foto|Cambiar foto/).click();
    await (await selector).setFiles(FOTO);
    await expect(hoja.locator('img')).toHaveAttribute('src', /^blob:/);
    await hoja.getByRole('button', {name: 'Guardar franja'}).click();
    await expect(page.getByText(/sale como franja en el inicio/)).toBeVisible();

    await page.goto(CATALOGO);
    await expect(page.locator('.band img')).toHaveAttribute('src', /\/api\/uploads\//);
  });

  test('producto nuevo con foto', async ({page}, info) => {
    await irEnElPanel(page, 'Productos');
    await page.getByRole('link', {name: '+ Nuevo producto'}).click();
    await page.getByLabel('Código').fill(`E2E-${info.project.name}-${Date.now() % 100000}`);
    await page.locator('#p-cat').selectOption({index: 1});
    await page.getByLabel('Nombre', {exact: true}).fill('Producto de prueba e2e');
    await page.locator('#p-precio').fill('12500');
    const selector = page.waitForEvent('filechooser');
    await page.getByText('Elegir foto').click();
    await (await selector).setFiles(FOTO);
    await page.locator('form button.btn-dark').click();
    await expect(page).toHaveURL(/\/admin\/productos\/\d+$/);
    await expect(page.locator('section img')).toHaveAttribute('src', /\/api\/uploads\//);
  });

  test('horario: el catálogo dice si el negocio está abierto', async ({page}, info) => {
    test.skip(info.project.name !== 'computador', 'una vez basta: cambia el horario del negocio');
    await irEnElPanel(page, 'Configuración');
    // Todo el día, todos los días: siempre abierto, sin depender de la hora en que corra la prueba.
    for (const dia of ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo']) {
      await page.getByRole('switch', {name: dia}).check();
      await page.getByLabel(`${dia}: abre a las`).fill('00:00');
      await page.getByLabel(`${dia}: cierra a las`).fill('23:59');
    }
    await page.getByRole('button', {name: 'Guardar cambios'}).click();
    await expect(page.getByText('Cambios guardados')).toBeVisible();

    await page.goto(CATALOGO + '/contacto');
    await expect(page.locator('.v-estado')).toHaveText('Abierto · cierra 11:59 p. m.');
    await expect(page.getByText('Lunes a domingo: 12:00 a. m. – 11:59 p. m.')).toBeVisible();
  });

  test('logo: el editor de recorte cabe en una pantalla baja y guarda', async ({page, isMobile}) => {
    test.skip(isMobile, 'la pantalla baja es la de un portátil');
    await page.setViewportSize({width: 1280, height: 720});
    await irEnElPanel(page, 'Configuración');
    const logo = page.getByRole('button', {name: 'Cambiar el logo'});
    // Sin logo, tocarlo abre el selector; con logo, el editor directo.
    const selector = page.waitForEvent('filechooser', {timeout: 3000}).catch(() => null);
    await logo.click();
    const elegido = await selector;
    if (elegido) await elegido.setFiles(FOTO);
    const guardar = page.getByRole('dialog').getByRole('button', {name: 'Guardar', exact: true});
    await expect(guardar).toBeVisible();
    const b = (await guardar.boundingBox())!;
    expect(b.y + b.height, 'el botón Guardar se ve sin hacer scroll').toBeLessThanOrEqual(720);
    await guardar.click();
    await expect(logo.locator('img')).toHaveAttribute('src', /\/api\/uploads\//);
  });
});

/** Super admin con la cuenta de prueba que crea el CI (E2E_SUPER_EMAIL / E2E_SUPER_PASSWORD). */
test.describe('super admin', () => {
  const email = process.env['E2E_SUPER_EMAIL'], temporal = process.env['E2E_SUPER_PASSWORD'];
  test.skip(!email || !temporal, 'sin cuenta de super admin de prueba');

  test('crea una empresa con su administrador', async ({page}, info) => {
    test.skip(info.project.name !== 'computador', 'una vez basta: cambia la contraseña de la cuenta');
    await entrarAlPanel(page, {email: email!, password: temporal!});
    // La contraseña inicial obliga a cambiarla.
    await expect(page).toHaveURL(/\/cambiar-contrasena$/);
    const nueva = temporal + 'x9';
    await page.locator('#f-actual').fill(temporal!);
    await page.locator('#f-nueva').fill(nueva);
    await page.locator('#f-confirmar').fill(nueva);
    await page.locator('form button.btn-dark').click();
    await expect(page).toHaveURL(/\/superadmin$/);

    const nombre = `Panadería E2E ${Date.now() % 100000}`;
    await page.getByRole('button', {name: '+ Nueva empresa'}).click();
    await page.locator('#f-nombre').fill(nombre);
    await page.locator('#f-admin').fill('Dueña E2E');
    await page.locator('#f-email').fill(`e2e-${Date.now()}@prueba.local`);
    await page.getByRole('button', {name: 'Crear'}).click();
    await expect(page.getByRole('heading', {name: `Administrador de ${nombre} creado`})).toBeVisible();
    await expect(page.locator('tbody tr').filter({hasText: nombre})).toBeVisible();
  });
});
