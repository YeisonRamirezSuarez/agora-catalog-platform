# Exporta el CSS del catálogo público del prototipo al front Angular, encerrado bajo .cat-root para que
# no choque con los estilos del panel (Tailwind). Toma las piezas base de index_base.html (variables,
# botones, foto, logo, hojas) y todo CAT_CSS de v3_css.py, con las variables que agrega build_v3.py.
# Uso (desde la raíz del repo):
#   python -X utf8 herramientas/prototipo/exportar_css_catalogo.py agora-catalog-front/src/app/modules/catalogo/catalogo.css
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from v3_css import CAT_CSS  # noqa: E402

SCOPE = '.cat-root'
base_html = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'index_base.html'), encoding='utf-8').read()
style = base_html[base_html.index('<style>') + 7:base_html.index('</style>')]
base = style[:style.index('/* ---------- Catálogo público ---------- */')]
# Las variables del catálogo que build_v3.py agrega a :root.
base = base.replace('  --ease:cubic-bezier(.16,1,.3,1);\n}',
                    '  --ease:cubic-bezier(.16,1,.3,1);\n'
                    '  --night:#15120D;--cream:#F7F1E6;--sand:#EFE4D0;--paper:#FFFDF9;\n'
                    '  --c-ink:#211C16;--c-ink-2:#4A4036;--c-muted:#6F655A;--c-line:#E7DDCC;--price:#B4182D;--on-accent:#231A0E;--accent:#DDB06C;\n'
                    "  --serif:'Playfair Display',Georgia,'Times New Roman',serif;--script:'Ms Madi',cursive;\n}")
css = base + CAT_CSS


def scope_selector(sel):
    sel = sel.strip()
    if sel.startswith('html'):
        return sel
    if sel in (':root', 'body', '*'):
        return SCOPE if sel != '*' else f'{SCOPE},{SCOPE} *'
    if sel.startswith('body'):
        return SCOPE + sel[4:]
    return f'{SCOPE} {sel}'


def split_selectors(prelude):
    # Comas de primer nivel: las de :not(a,b) o :is(a,b) no separan selectores.
    parts, depth, cur = [], 0, ''
    for ch in prelude:
        depth += {'(': 1, ')': -1}.get(ch, 0)
        if ch == ',' and depth == 0:
            parts.append(cur)
            cur = ''
        else:
            cur += ch
    return parts + [cur]


def scope(text):
    out, i = [], 0
    while i < len(text):
        j = text.find('{', i)
        if j < 0:
            out.append(text[i:])
            break
        head = text[i:j]
        # comentarios sueltos antes del selector se conservan tal cual
        comments = ''.join(re.findall(r'/\*.*?\*/\s*', head, re.S))
        prelude = re.sub(r'/\*.*?\*/', '', head, flags=re.S).strip()
        depth, k = 1, j + 1
        while depth:
            depth += {'{': 1, '}': -1}.get(text[k], 0)
            k += 1
        body = text[j + 1:k - 1]
        if prelude.startswith('@keyframes'):
            out.append(f'{comments}{prelude}{{{body}}}\n')
        elif prelude.startswith('@'):
            out.append(f'{comments}{prelude}{{\n{scope(body)}}}\n')
        else:
            sels = ','.join(scope_selector(s) for s in split_selectors(prelude))
            out.append(f'{comments}{sels}{{{body.strip()}}}\n')
        i = k
    return ''.join(out)


result = ('/* Generado por herramientas/prototipo/exportar_css_catalogo.py desde el prototipo. No editar a mano:\n'
          '   cambia el prototipo y vuelve a exportar. */\n'
          f'{SCOPE}{{font:15px/1.45 var(--font);-webkit-font-smoothing:antialiased}}\n'
          f'{SCOPE} svg{{display:inline-block}}\n'
          + scope(css))
open(sys.argv[1], 'w', encoding='utf-8', newline='\n').write(result)
print(f'{sys.argv[1]}: {len(result)} bytes')
