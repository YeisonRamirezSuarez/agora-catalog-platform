# Toma capturas de cada pantalla estática (móvil y computador) y las une en un PDF para subirlo a un chat de IA.
# Uso: python capturas_pdf.py <docs/prototipo> [base_url]
import io
import pathlib
import sys
from PIL import Image
from playwright.sync_api import sync_playwright

ROOT = pathlib.Path(sys.argv[1])
BASE = sys.argv[2] if len(sys.argv) > 2 else 'http://127.0.0.1:2020/'
OUT = ROOT / 'capturas'
CHROME = r'C:\Program Files\Google\Chrome\Application\chrome.exe'
ORDER = ['catalogo', 'catalogo-lista', 'catalogo-producto', 'catalogo-pedido', 'catalogo-whatsapp', 'login', 'panel', 'productos',
         'producto', 'categorias', 'colecciones', 'importar', 'configuracion', 'configuracion-recorte', 'superadmin']
SHEET = ('-producto', '-pedido', '-whatsapp', '-recorte')

OUT.mkdir(exist_ok=True)
for old in OUT.glob('*.png'):
    old.unlink()  # sin capturas de pantallas que ya no existen
pages = []
with sync_playwright() as pw:
    b = pw.chromium.launch(executable_path=CHROME, headless=True)
    for w, h, tag in [(390, 844, 'movil'), (1440, 900, 'computador')]:
        p = b.new_page(viewport={'width': w, 'height': h}, device_scale_factor=1)
        for name in ORDER:
            p.goto(f'{BASE}estatico/{name}.html', wait_until='networkidle')
            # una captura de página completa no baja por la página: las fotos diferidas se cargan antes
            p.evaluate("document.querySelectorAll('img[loading=lazy]').forEach(i => { i.loading = 'eager'; })")
            p.wait_for_function('[...document.images].every(i => i.complete)', timeout=30000)
            p.evaluate('document.fonts.ready')
            p.wait_for_timeout(1200)  # animación de entrada de la portada
            # las pantallas con hoja abierta se capturan solo en la ventana visible; el resto, completas
            png = p.screenshot(full_page=not name.endswith(SHEET))
            (OUT / f'{name}-{tag}.png').write_bytes(png)
            pages.append(Image.open(io.BytesIO(png)).convert('RGB'))
            print(name, tag)
        p.close()
    b.close()

assert len(pages) == 2 * len(ORDER)
pages[0].save(OUT / 'prototipo-agora-catalog.pdf', save_all=True, append_images=pages[1:], resolution=96)
print('pdf', (OUT / 'prototipo-agora-catalog.pdf').stat().st_size // 1024, 'KB')
