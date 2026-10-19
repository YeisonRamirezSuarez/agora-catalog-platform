# Exporta cada pantalla del prototipo como HTML estático (sin JavaScript) para lectores que no ejecutan JS.
# Uso: python export_static.py <docs/prototipo> [base_url]
import pathlib
import re
import sys
from playwright.sync_api import sync_playwright

ROOT = pathlib.Path(sys.argv[1])
BASE = sys.argv[2] if len(sys.argv) > 2 else 'http://127.0.0.1:2020/'
OUT = ROOT / 'estatico'
CHROME = r'C:\Program Files\Google\Chrome\Application\chrome.exe'
CAT = '#/c/salsamentaria-javier-x7k2m9ab'
ADD = ('#fv-row .pc:nth-child(1) [data-act=add]', '#fv-row .pc:nth-child(2) [data-act=add]', '.cart-btn')
# archivo, ruta, clics en orden, título para el índice, descripción
PAGES = [
    ('catalogo.html', CAT, (), 'Inicio del catálogo', 'Lo que ve el cliente al abrir el link: portada con la frase, planes, favoritos y combos.'),
    ('catalogo-lista.html', CAT + '/catalogo', (), 'Catálogo con filtros', 'Todos los productos con búsqueda, filtros por categoría, precio y promociones, y orden.'),
    ('catalogo-producto.html', CAT + '/catalogo', ('#results .pc-hit[data-id="11"]',), 'Producto abierto', 'Hoja con foto grande, cantidad, «Agregar al pedido» y pregunta por WhatsApp.'),
    ('catalogo-pedido.html', CAT, ADD, 'Mi pedido', 'El pedido se arma en el navegador del cliente y se envía por WhatsApp con cantidades y total.'),
    ('catalogo-whatsapp.html', CAT, ADD + ('#o-form .btn-brand',), 'Elegir número de WhatsApp', 'El negocio tiene dos números: el cliente elige a cuál enviar el pedido.'),
    ('login.html', '#/login', (), 'Ingreso al panel', 'Login del dueño del negocio.'),
    ('panel.html', '#/admin', (), 'Inicio del panel', 'Resumen, link público y código QR.'),
    ('productos.html', '#/admin/productos', (), 'Productos', 'Lista con edición rápida de precio y disponibilidad.'),
    ('producto.html', '#/admin/producto/7', (), 'Editar producto', 'Formulario con oferta por fechas y foto.'),
    ('categorias.html', '#/admin/categorias', (), 'Categorías', 'Ordenar, renombrar, ocultar y elegir la franja del inicio.'),
    ('colecciones.html', '#/admin/colecciones', (), 'Colecciones', 'Los planes de «¿Cuál es el plan?» en el inicio del catálogo.'),
    ('importar.html', '#/admin/importar', (), 'Importar desde Excel', 'Plantilla, carga, revisión de errores y fotos en ZIP.'),
    ('configuracion.html', '#/admin/configuracion', (), 'Configuración', 'Datos, WhatsApp, portada con su frase, sellos, colores y vista previa.'),
    ('configuracion-recorte.html', '#/admin/configuracion', ('#cprev .pv-cover',), 'Recortar portada', 'Editor de recorte: arrastrar y zoom; la franja marca lo que se ve en el celular.'),
    ('superadmin.html', '#/superadmin', (), 'Super admin', 'Empresas de la plataforma.'),
]
STATIC_OF = {r: f for f, r, a, *_ in PAGES if not a}


def static_href(m):
    route = m.group(1)
    if route.startswith('#/c/'):
        target = 'catalogo-lista.html' if '/catalogo' in route else 'catalogo.html'
    elif route.startswith('#/admin/producto/'):
        target = 'producto.html'
    elif route.startswith('#/admin/coleccion/'):
        target = 'colecciones.html'
    else:
        target = STATIC_OF.get(route, 'index.html')
    return f'href="estatico/{target}"'


OUT.mkdir(exist_ok=True)
with sync_playwright() as pw:
    b = pw.chromium.launch(executable_path=CHROME, headless=True)
    for name, route, clicks, *_ in PAGES:
        ctx = b.new_context(viewport={'width': 1280, 'height': 900})  # contexto limpio: el pedido de una página no pasa a la otra
        p = ctx.new_page()
        p.goto(BASE + '?static=' + name[:-5] + route)
        p.wait_for_timeout(700)
        for sel in clicks:
            p.locator(sel).first.click()
            p.wait_for_timeout(500)
        html = p.evaluate('document.documentElement.outerHTML')
        html = re.sub(r'<script\b[^>]*>.*?</script>', '', html, flags=re.S)
        # sin JS el encabezado no se vuelve sólido al bajar: se queda arriba en vez de flotar sobre el contenido
        html = html.replace('<head>', '<head><base href="../">', 1).replace('</head>', '<style>.top{position:absolute}</style></head>', 1)
        html = re.sub(r'href="(#/[^"]*)"', static_href, html)
        html = re.sub(r'<noscript>.*?</noscript>', '', html, flags=re.S)
        (OUT / name).write_text('<!doctype html>\n' + html, encoding='utf-8')
        print(name)
        ctx.close()
    b.close()

items = '\n'.join(f'<li><a href="{f}">{t}</a><span>{d}</span></li>' for f, r, a, t, d in PAGES)
(OUT / 'index.html').write_text(f'''<!doctype html>
<html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Prototipo Agorá Catalog: versión estática</title>
<link rel="icon" type="image/svg+xml" href="../marca/favicon.svg">
<style>
body{{margin:0;background:#fff;color:#1C1814;font:16px/1.55 system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}}
main{{max-width:760px;margin:0 auto;padding:40px 16px 64px}}
img{{height:28px;width:auto}}
h1{{margin:24px 0 8px;font:600 30px/1.15 'Playfair Display',Georgia,serif}}
p{{margin:0 0 12px;color:#6B6259}}
ul{{margin:24px 0 0;padding:0;list-style:none;border-top:1px solid #ECE6DC}}
li{{display:grid;gap:2px;padding:14px 0;border-bottom:1px solid #ECE6DC}}
a{{color:#8E5F18;font-weight:600}}
li span{{color:#6B6259;font-size:15px}}
</style></head>
<body><main>
<img src="../marca/logo.svg" alt="Agorá Catalog">
<h1>Prototipo: catálogo de Salsamentaria Javier</h1>
<p>Versión estática de cada pantalla, para leerla sin JavaScript. Los botones no funcionan aquí.</p>
<p>Versión interactiva (necesita un navegador con JavaScript): <a href="../">abrir el prototipo</a>.</p>
<ul>
{items}
</ul>
</main></body></html>
''', encoding='utf-8')
print('index.html')
