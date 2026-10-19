# Herramientas del prototipo Agorá Catalog

Scripts para regenerar el prototipo de `docs/prototipo/`, su versión estática, el PDF de capturas y el logo. Antes vivían en una carpeta temporal de Claude Code.

## Requisitos

- Python 3.14 con `playwright`, `Pillow`, `fontTools` y `skia-pathops` (`pip install --user playwright pillow fonttools skia-pathops`).
- Google Chrome en `C:\Program Files\Google\Chrome\Application\chrome.exe`. Los scripts lo abren sin ventana.
- No ejecutes los scripts con `python -I`: ese modo oculta los paquetes instalados con `--user`.

Todos los comandos de abajo se ejecutan desde la raíz del proyecto.

## prototipo/: construir y publicar

| Archivo | Para qué sirve |
|---|---|
| `index_base.html` | Base del prototipo (panel, datos de ejemplo, router). |
| `v3_css.py` | CSS del catálogo público y del panel. `PANEL_BRAND` contiene la marca Agorá del panel. |
| `v3_js.py` | JavaScript del catálogo público, el pie de página y la página «Catálogo no disponible». |
| `build_v3.py` | Une los tres archivos anteriores en un solo HTML. |
| `export_static.py` | Exporta cada pantalla como HTML sin JavaScript en `docs/prototipo/estatico/`. |
| `capturas_pdf.py` | Captura las páginas estáticas y crea `docs/prototipo/capturas/prototipo-agora-catalog.pdf`. |

Pasos para publicar una versión nueva:

1. Edita `index_base.html`, `v3_css.py`, `v3_js.py` o `build_v3.py`.
2. Construye la versión nueva:
   `python -X utf8 herramientas/prototipo/build_v3.py herramientas/prototipo/index_base.html docs/prototipo/v11.html`
3. Copia `docs/prototipo/v11.html` sobre `docs/prototipo/index.html`.
4. Levanta un servidor local en otra terminal:
   `python -m http.server 2021 --bind 127.0.0.1 --directory docs/prototipo`
5. Ejecuta las pruebas (sección siguiente).
6. Regenera la versión estática y el PDF:
   `python -X utf8 herramientas/prototipo/export_static.py docs/prototipo http://127.0.0.1:2021/`
   `python -X utf8 herramientas/prototipo/capturas_pdf.py docs/prototipo http://127.0.0.1:2021/`

El link público del panel está fijo en `build_v3.py` (`publicUrl`, dominio `e-catalog-platform.duckdns.org`). Cámbialo ahí cuando se migre DuckDNS.

## pruebas/: verificar antes de publicar

Todas necesitan el servidor local del paso 4.

| Archivo | Comando | Qué revisa |
|---|---|---|
| `flows.py` | `python -X utf8 herramientas/pruebas/flows.py http://127.0.0.1:2021/` | Los flujos completos: pedido, panel, importación y super admin. Debe terminar en «FALLAS: ninguna». |
| `contract_check.py` | `python -X utf8 herramientas/pruebas/contract_check.py http://127.0.0.1:2021/ <carpeta_capturas>` | Anchos de 360 a 1920, recortes en tarjetas, foco y Escape, movimiento reducido. |
| `audit_shots.py` | `python -X utf8 herramientas/pruebas/audit_shots.py http://127.0.0.1:2021/ <carpeta_capturas> [ronda]` | Capturas de 13 rutas en 5 anchos, errores de consola, imágenes rotas, controles sin nombre y desborde. |
| `focus_shot.py` | `python -X utf8 herramientas/pruebas/focus_shot.py http://127.0.0.1:2021/ <carpeta_capturas>` | El foco visible en el login y en el menú del panel. |
| `brand_shots.py` | `python -X utf8 herramientas/pruebas/brand_shots.py docs/prototipo <carpeta_capturas>` | Capturas de los lugares donde aparece el logo. Levanta su propio servidor en el puerto 2021, así que no lo uses con el servidor del paso 4 abierto. |

## marca/ y font/: logo Sello AC

- `marca/agora_mono.py` dibuja el monograma AC con fontTools y skia-pathops.
- `marca/agora_sello_kit.py` genera todos los SVG del kit y `site.webmanifest`:
  `python -X utf8 herramientas/marca/agora_sello_kit.py <carpeta_salida>`
- `font/` contiene las instancias de Playfair Display que usa el dibujo. Salen de los archivos variables de google/fonts y tienen licencia SIL Open Font License.

La salida de `agora_sello_kit.py` es igual byte a byte a los SVG de `docs/prototipo/marca/`.

Los PNG y el `favicon.ico` del kit no tienen exportador propio en esta carpeta. Si cambias los SVG, expórtalos de nuevo con cualquier conversor de SVG a PNG:

| SVG de origen | PNG que genera |
|---|---|
| `icon.svg` | `icon-512.png` (512) |
| `icon-premium.svg` | `icon-premium-1024.png` (1024) |
| `app-icon.svg` | `app-icon-192.png` (192) y `app-icon-512.png` (512) |
| `app-icon-square.svg` | `maskable-512.png` (512) y `apple-touch-icon.png` (180) |
| `favicon.svg` | `favicon-16.png`, `favicon-32.png`, `favicon-48.png` y `favicon.ico` (16, 32 y 48) |
| `logo.svg` | `logo@2x.png` (1134×240) |
| `logo-stacked.svg` | `logo-stacked@2x.png` (912×660) |
