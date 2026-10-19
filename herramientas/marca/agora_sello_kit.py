# Kit final del logo de Agorá Catalog: el Sello (monograma AC tejido delante de un aro fino).
# Uso: python agora_sello_kit.py <dir_salida>
# Dos dibujos del símbolo:
#   master  (aro 5, cortes 4)   para 48 px o más: símbolo, logo apilado, presentación.
#   pequeño (aro 12, cortes 6.5) para menos de 48 px: logo horizontal de la app, ícono de app y favicon.
import json
import os
import sys

import agora_mono as m
from agora_mono import CREAM, GOLD, INK, D, U, circle, clean, grown, path, svg, word, xf

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)
m.OUT = OUT
V = '0 0 256 256'


def put(name, view, body, defs=''):
    open(os.path.join(OUT, name), 'w', encoding='utf-8').write(svg(view, body, defs=defs))


def sello(gap, ring_w, ring_r):
    m.GAP = gap  # monogram() lee el corte global
    A, C, shadows, n = m.monogram()
    assert n == 4, f'el tejido debe tener 4 cruces, tiene {n}'
    ring = D(circle(128, 138, ring_r + ring_w / 2), circle(128, 138, ring_r - ring_w / 2))
    ring = clean(D(ring, grown(U(A, C), gap + 2)), 4)
    return U(A, ring), C, shadows


def premium(ink_all, C, shadows, ink_fill, key):
    sh = ''
    for i, (under, over) in enumerate(shadows):
        target = C if under == 'C' else ink_all
        cid = f'{key}{i}'
        colour = m.GOLD_DEEP if under == 'C' else '#000000'
        op = '.2' if under == 'A' and ink_fill == CREAM else '.55'
        sh += (f'<clipPath id="{cid}"><path d="{m.svgd(target)}"/></clipPath>'
               f'<g clip-path="url(#{cid})"><path fill="{colour}" opacity="{op}" filter="url(#soft)" '
               f'd="{m.svgd(grown(over, m.GAP + 1.5))}"/></g>')
    return path(ink_all, ink_fill) + path(C, 'url(#foil)') + sh


# ---------- símbolo ----------
INK_M, C_M, SH_M = sello(4.0, 5, 110)
GAP_M = m.GAP
INK_S, C_S, _ = sello(6.5, 12, 107)

put('icon.svg', V, path(INK_M, INK) + path(C_M, GOLD))
put('icon-black.svg', V, path(U(INK_M, C_M), '#000000'))
put('icon-white.svg', V, path(U(INK_M, C_M), '#ffffff'))
put('icon-dark.svg', V, path(INK_M, CREAM) + path(C_M, GOLD))
m.GAP = GAP_M
put('icon-premium.svg', V, premium(INK_M, C_M, SH_M, INK, 'p'), m.FOIL)
put('icon-premium-dark.svg', V, premium(INK_M, C_M, SH_M, CREAM, 'q'), m.FOIL)
put('icon-small.svg', V, path(INK_S, INK) + path(C_S, GOLD))
put('icon-small-white.svg', V, path(U(INK_S, C_S), '#ffffff'))


def on_tile(k, rx):
    t = (k, 0, 0, k, 128 * (1 - k), 128 * (1 - k) - 4 * k)  # sube 4 unidades: el aro está centrado en y=138
    return (f'<rect width="256" height="256"{f" rx={chr(34)}{rx}{chr(34)}" if rx else ""} fill="{INK}"/>'
            + path(xf(INK_S, t), CREAM) + path(xf(C_S, t), GOLD))


put('app-icon.svg', V, on_tile(0.80, 56))       # favicon e ícono redondeado
put('app-icon-square.svg', V, on_tile(0.68, 0))  # iOS y Android recortan la esquina; deja zona segura

# ---------- nombre ----------
wi, wg, w, cap = word(m.NAME, 'Playfair_Display_wght_600.ttf', -0.005)


def horizontal():
    H, CAP = 256, 256 * 0.40
    s = CAP / cap
    x0, base = 256 + 26, 128 + CAP / 2 + 8
    tf = (s, 0, 0, -s, x0, base)
    return xf(wi, tf), xf(wg, tf), round(x0 + w * s + 8)


WI, WG, total = horizontal()
hv = f'0 0 {total} 256'
put('logo.svg', hv, path(INK_S, INK) + path(C_S, GOLD) + path(WI, INK) + path(WG, GOLD))
put('logo-dark.svg', hv, path(INK_S, CREAM) + path(C_S, GOLD) + path(WI, CREAM) + path(WG, GOLD))
put('logo-black.svg', hv, path(U(INK_S, C_S, WI, WG), '#000000'))
put('logo-white.svg', hv, path(U(INK_S, C_S, WI, WG), '#ffffff'))

# Solo el nombre, con su propia caja.
s = 100 / cap
NI, NG = xf(wi, (s, 0, 0, -s, 6, 118)), xf(wg, (s, 0, 0, -s, 6, 118))
b = U(NI, NG).bounds
wv = f'0 {int(b[1]) - 6} {round(b[2] + 12)} {round(b[3] - b[1]) + 12}'
put('wordmark.svg', wv, path(NI, INK) + path(NG, GOLD))
put('wordmark-white.svg', wv, path(U(NI, NG), '#ffffff'))

# Apilado: sello master arriba, nombre en mayúsculas espaciadas debajo.
si, sg, sw, scap = word('AGORÁ CATALOG', 'Playfair_Display_wght_700.ttf', 0.16)
SC = 30
ss = SC / scap
sx = 128 - sw * ss / 2
stf = (ss, 0, 0, -ss, sx, 256 + 30 + SC)
SI, SG = xf(si, stf), xf(sg, stf)
left = int(min(0, sx - 8))
sv = f'{left} 0 {256 - 2 * left} {256 + 30 + SC + 14}'
put('logo-stacked.svg', sv, path(INK_M, INK) + path(C_M, GOLD) + path(SI, INK) + path(SG, GOLD))
put('logo-stacked-dark.svg', sv, path(INK_M, CREAM) + path(C_M, GOLD) + path(SI, CREAM) + path(SG, GOLD))
put('logo-stacked-black.svg', sv, path(U(INK_M, C_M, SI, SG), '#000000'))
put('logo-stacked-white.svg', sv, path(U(INK_M, C_M, SI, SG), '#ffffff'))
put('logo-stacked-premium.svg', sv, premium(INK_M, C_M, SH_M, INK, 'r') + path(SI, INK) + path(SG, GOLD), m.FOIL)

json.dump({'name': 'Agorá Catalog', 'short_name': 'Agorá', 'theme_color': INK, 'background_color': CREAM, 'display': 'standalone',
           'icons': [{'src': 'app-icon-192.png', 'sizes': '192x192', 'type': 'image/png'},
                     {'src': 'app-icon-512.png', 'sizes': '512x512', 'type': 'image/png'},
                     {'src': 'maskable-512.png', 'sizes': '512x512', 'type': 'image/png', 'purpose': 'maskable'}]},
          open(os.path.join(OUT, 'site.webmanifest'), 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
print('ok', 'horizontal', hv, 'apilado', sv, 'nombre', wv)

# Favicon de 16 px: a ese tamaño el tejido se empasta, así que queda la silueta del sello:
# la A en crema rompiendo un aro dorado grueso, sin la C.
Ag = m.glyph(m.UP, 'A')
ab = Ag.bounds
sa = 196 / (ab[3] - ab[1])
FA = xf(Ag, (sa, 0, 0, -sa, 128 - sa * (ab[0] + ab[2]) / 2, 226 + sa * ab[1]))
FR = D(circle(128, 132, 96), circle(128, 132, 74))
FR = clean(D(FR, grown(FA, 10)), 4)
put('favicon.svg', V, f'<rect width="256" height="256" rx="56" fill="{INK}"/>' + path(FA, CREAM) + path(FR, GOLD))
