# Auditoría visual y técnica de todas las pantallas del prototipo.
# Uso: python audit_shots.py <url_base> <dir_salida> [ronda]
# Captura cada ruta en 1920, 1280, 768 y 390; anota errores de consola, peticiones fallidas, imágenes rotas,
# controles sin nombre accesible, campos sin etiqueta, desborde horizontal y si el foco se ve.
import json
import os
import sys
from playwright.sync_api import sync_playwright

base, out = sys.argv[1].rstrip('/') + '/', sys.argv[2]
tag = sys.argv[3] if len(sys.argv) > 3 else 'a0'
os.makedirs(out, exist_ok=True)
C = '#/c/salsamentaria-javier-x7k2m9ab'
ROUTES = [('home', C), ('catalogo', C + '/catalogo'), ('login', '#/login'), ('admin', '#/admin'),
          ('productos', '#/admin/productos'), ('producto-nuevo', '#/admin/producto/nuevo'),
          ('categorias', '#/admin/categorias'), ('importar', '#/admin/importar'),
          ('configuracion', '#/admin/configuracion'), ('colecciones', '#/admin/colecciones'), ('coleccion-nueva', '#/admin/coleccion/nueva'),
          ('superadmin', '#/superadmin'), ('nodisp', '#/c/no-existe-123')]
SIZES = [(1920, 1080), (1280, 800), (768, 1024), (390, 844), (360, 780)]
A11Y = """() => {
  const vis = e => e.offsetParent !== null || e.getClientRects().length;
  const name = e => (e.getAttribute('aria-label') || e.getAttribute('title') || e.textContent || e.querySelector('img[alt]')?.alt || '').trim();
  const unnamed = [...document.querySelectorAll('button, a[href], [role=button]')].filter(vis).filter(e => !name(e)).map(e => e.outerHTML.slice(0, 90));
  const unlabeled = [...document.querySelectorAll('input, select, textarea')].filter(vis).filter(e => e.type !== 'hidden' && e.type !== 'file')
    .filter(e => !(e.labels && e.labels.length) && !e.getAttribute('aria-label') && !e.getAttribute('aria-labelledby')).map(e => e.outerHTML.slice(0, 90));
  const broken = [...document.images].filter(i => i.complete && !i.naturalWidth && i.src).map(i => i.src);
  const small = [...document.querySelectorAll('button, a[href], input, select')].filter(vis).filter(e => { const r = e.getBoundingClientRect(); return r.width && r.height && (r.width < 24 || r.height < 24); }).map(e => (e.getAttribute('aria-label') || e.textContent || e.tagName).trim().slice(0, 30));
  return {unnamed, unlabeled, broken, small: small.slice(0, 8), scrollW: document.documentElement.scrollWidth, w: innerWidth};
}"""
report = {}
with sync_playwright() as pw:
    b = pw.chromium.launch(executable_path=r'C:\Program Files\Google\Chrome\Application\chrome.exe')
    for w, h in SIZES:
        ctx = b.new_context(viewport={'width': w, 'height': h})
        p = ctx.new_page()
        cons, failed = [], []
        p.on('console', lambda m: cons.append(f'{m.type}: {m.text[:160]}') if m.type in ('error', 'warning') else None)
        p.on('pageerror', lambda e: cons.append(f'pageerror: {e}'))
        p.on('requestfailed', lambda r: failed.append(r.url))
        p.on('response', lambda r: failed.append(f'{r.status} {r.url}') if r.status >= 400 else None)
        p.goto(base + C)
        p.wait_for_timeout(1200)
        for name, r in ROUTES:
            p.evaluate(f'location.hash = "{r}"')
            p.wait_for_timeout(700)
            p.screenshot(path=os.path.join(out, f'{tag}-{name}-{w}.png'), full_page=(w in (1280, 390, 360)))
            report[f'{name}-{w}'] = p.evaluate(A11Y)
        # Foco visible: tabula 3 veces en el login y mide el contorno del elemento enfocado.
        p.evaluate('location.hash = "#/login"'); p.wait_for_timeout(500)
        foc = []
        for _ in range(4):
            p.keyboard.press('Tab')
            foc.append(p.evaluate("""() => { const e = document.activeElement, s = getComputedStyle(e);
              return [e.tagName, (e.textContent||e.name||'').trim().slice(0,20), s.outlineStyle, s.outlineWidth, s.boxShadow.slice(0,40)]; }"""))
        report[f'focus-login-{w}'] = foc
        report[f'console-{w}'] = cons
        report[f'failed-{w}'] = sorted(set(failed))
        ctx.close()
    b.close()
json.dump(report, open(os.path.join(out, f'{tag}-report.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
for k, v in report.items():
    if isinstance(v, dict):
        issues = {kk: vv for kk, vv in v.items() if vv and kk not in ('scrollW', 'w')}
        if v['scrollW'] > v['w'] + 1:
            issues['overflow'] = v['scrollW']
        if issues:
            print(k, json.dumps(issues, ensure_ascii=False)[:400])
    elif v:
        print(k, json.dumps(v, ensure_ascii=False)[:400])
print('ok')
