# Capturas de los puntos donde aparece la marca Agorá Catalog. Uso: python brand_shots.py <carpeta_prototipo> <salida>
import pathlib, subprocess, sys, time
from playwright.sync_api import sync_playwright
P, OUT = sys.argv[1], pathlib.Path(sys.argv[2]); OUT.mkdir(exist_ok=True)
srv = subprocess.Popen([sys.executable, '-m', 'http.server', '2021', '--bind', '127.0.0.1', '--directory', P], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
time.sleep(1.2)
try:
    with sync_playwright() as pw:
        b = pw.chromium.launch(executable_path=r'C:\Program Files\Google\Chrome\Application\chrome.exe', headless=True)
        for name, w, h, route, sel in [('foot-1920', 1920, 1000, '#/c/salsamentaria-javier-x7k2m9ab', '.foot'), ('foot-390', 390, 844, '#/c/salsamentaria-javier-x7k2m9ab', '.foot'),
                                       ('login-1280', 1280, 800, '#/login', 'body'), ('admin-1280', 1280, 800, '#/admin', 'body'), ('menu-390', 390, 844, '#/c/salsamentaria-javier-x7k2m9ab', None)]:
            p = b.new_page(viewport={'width': w, 'height': h})
            p.goto(f'http://127.0.0.1:2021/{route}'); p.wait_for_timeout(900)
            if sel is None:
                p.click('[data-act="menu"]'); p.wait_for_timeout(500); p.screenshot(path=str(OUT / f'{name}.png'))
            elif sel == 'body':
                p.screenshot(path=str(OUT / f'{name}.png'))
            else:
                p.locator(sel).screenshot(path=str(OUT / f'{name}.png'))
            p.close()
        b.close()
finally:
    srv.terminate()
print('ok')
