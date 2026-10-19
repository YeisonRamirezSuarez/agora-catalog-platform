# Foco visible tras la transición: captura el login con el campo y el botón enfocados, y el menú del panel.
import os, sys
from playwright.sync_api import sync_playwright
base, out = sys.argv[1], sys.argv[2]
os.makedirs(out, exist_ok=True)
with sync_playwright() as pw:
    b = pw.chromium.launch(executable_path=r'C:\Program Files\Google\Chrome\Application\chrome.exe')
    p = b.new_page(viewport={'width': 1280, 'height': 800})
    p.goto(base + '#/login'); p.wait_for_timeout(800)
    p.keyboard.press('Tab'); p.wait_for_timeout(400)
    print('input', p.evaluate("getComputedStyle(document.activeElement).boxShadow + ' | ' + getComputedStyle(document.activeElement).borderColor"))
    p.screenshot(path=f'{out}/focus-input.png', clip={'x': 340, 'y': 150, 'width': 600, 'height': 500})
    p.keyboard.press('Tab'); p.keyboard.press('Tab'); p.wait_for_timeout(300)
    print('button', p.evaluate("document.activeElement.textContent.trim() + ' | ' + getComputedStyle(document.activeElement).outline"))
    p.screenshot(path=f'{out}/focus-button.png', clip={'x': 340, 'y': 150, 'width': 600, 'height': 500})
    p.goto(base + '#/admin'); p.wait_for_timeout(800)
    for _ in range(3): p.keyboard.press('Tab')
    p.wait_for_timeout(300)
    print('nav', p.evaluate("document.activeElement.textContent.trim().slice(0,30) + ' | ' + getComputedStyle(document.activeElement).outline"))
    p.screenshot(path=f'{out}/focus-nav.png', clip={'x': 0, 'y': 0, 'width': 640, 'height': 400})
    b.close()
