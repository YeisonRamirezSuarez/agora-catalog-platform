# Revisa el prototipo contra REGLAS-DISEÑO.md: anchos 360-1440, recortes en tarjetas, foco y Escape, movimiento reducido.
# Uso: python contract_check.py <url_página> <dir_capturas> [ronda]
import os
import sys
from playwright.sync_api import sync_playwright

base, out = sys.argv[1], sys.argv[2]
tag = sys.argv[3] if len(sys.argv) > 3 else 'c1'
os.makedirs(out, exist_ok=True)
C = '#/c/salsamentaria-javier-x7k2m9ab'
fails, errs = [], []

# Una tarjeta está "recortada" si algún hijo visible se sale de su caja.
CLIP = """sel => [...document.querySelectorAll(sel)].slice(0, 6).flatMap(c => {
  const r = c.getBoundingClientRect();
  return [...c.querySelectorAll('button, .price, .pres, h3, output')].filter(e => e.offsetParent).map(e => e.getBoundingClientRect())
    .filter(b => b.left < r.left - 1 || b.right > r.right + 1).map(b => Math.round(b.right - r.right));
})"""


def check(ok, what):
    print(('OK   ' if ok else 'FAIL ') + what)
    if not ok:
        fails.append(what)


with sync_playwright() as pw:
    b = pw.chromium.launch(executable_path=r'C:\Program Files\Google\Chrome\Application\chrome.exe', headless=True)
    for w, h in ((360, 740), (390, 844), (768, 1024), (1024, 768), (1280, 800), (1440, 900), (1920, 1080)):
        mob = w < 768
        ctx = b.new_context(viewport={'width': w, 'height': h}, is_mobile=mob, has_touch=mob)
        p = ctx.new_page()
        p.on('pageerror', lambda e: errs.append(str(e)))
        p.goto(base + C)
        p.evaluate('document.fonts.ready')
        p.wait_for_timeout(1500)
        check(p.evaluate('document.documentElement.scrollWidth') <= w, f'{w}: inicio sin scroll horizontal')
        hero = p.locator('.hero').bounding_box()
        first = p.evaluate("Math.round(document.querySelector('.circles, .planes, .plan').getBoundingClientRect().top)")
        print(f'     {w}: portada {round(hero["height"])}px; primer bloque de productos/planes a {first}px (pantalla {h})')
        check(p.evaluate(CLIP, '#fv-row .pc') == [], f'{w}: tarjetas de favoritos sin controles recortados')
        p.screenshot(path=os.path.join(out, f'{tag}-{w}-inicio.png'))
        p.goto(base + C + '/catalogo')
        p.wait_for_timeout(900)
        check(p.evaluate('document.documentElement.scrollWidth') <= w, f'{w}: catálogo sin scroll horizontal')
        check(p.evaluate(CLIP, '#results .pc') == [], f'{w}: tarjetas del catálogo sin controles recortados')
        p.screenshot(path=os.path.join(out, f'{tag}-{w}-catalogo.png'))
        # Foco: el pedido (y el menú en celular) se cierran con Escape y el foco vuelve al botón que los abrió
        for opener in (['[data-act=menu]'] if mob else []) + ['.cart-btn']:
            p.locator(opener).focus()
            p.keyboard.press('Enter')
            p.wait_for_timeout(450)
            opened = p.locator('.sheet').count() == 1
            p.keyboard.press('Escape')
            p.wait_for_timeout(450)
            back = p.evaluate(f"document.activeElement === document.querySelector('{opener}')")
            check(opened and back and p.locator('.sheet').count() == 0, f'{w}: {opener} abre con Enter, cierra con Escape y devuelve el foco')
        ctx.close()
    # Movimiento reducido: Contáctanos baja sin animación y sin errores
    ctx = b.new_context(viewport={'width': 1440, 'height': 900}, reduced_motion='reduce')
    p = ctx.new_page()
    p.on('pageerror', lambda e: errs.append(str(e)))
    p.goto(base + C + '/catalogo')
    p.wait_for_timeout(900)
    p.locator('.nav a[href$="/contacto"]').click()
    p.wait_for_timeout(250)
    top = p.locator('#contacto').evaluate('e => e.getBoundingClientRect().top')
    check(0 <= top < 300, 'movimiento reducido: Contáctanos baja de inmediato')
    ctx.close()
    b.close()
print('errores de página:', errs or 'ninguno')
print('FALLAS:', fails or 'ninguna')
