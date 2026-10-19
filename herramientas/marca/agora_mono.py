# Monograma AC de Agorá Catalog (dirección C elegida por el usuario), tres refinamientos:
#   1 Tejido: A de Playfair Display Black y C de Playfair Display Black Italic, entrelazadas.
#   2 Grabado: el tejido con una línea interior grabada en los trazos gruesos.
#   3 Sello: el tejido delante de un aro fino que la A rompe por arriba.
# Cada uno sale en versión plana (master editable) y en versión "premium" (dorado lámina y sombra en los cruces).
# Uso: python agora_mono.py <dir_salida>
import math
import os
import sys
from fontTools.pens.basePen import BasePen
from fontTools.pens.recordingPen import DecomposingRecordingPen
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont
from pathops import LineCap, LineJoin, Path, PathOp, op

OUT = sys.argv[1] if len(sys.argv) > 1 else '.'
FONTS = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'font')
INK, GOLD, CREAM, GOLD_DEEP = '#1C1814', '#E0A43A', '#FBF6EA', '#8E5F18'
NAME = 'Agorá Catalog'


class ToPath(BasePen):
    def __init__(self, path):
        super().__init__(None)
        self.p = path.getPen()

    def _moveTo(self, pt): self.p.moveTo(pt)
    def _lineTo(self, pt): self.p.lineTo(pt)
    def _curveToOne(self, a, b, c): self.p.curveTo(a, b, c)
    def _qCurveToOne(self, a, b): self.p.qCurveTo(a, b)
    def _closePath(self): self.p.closePath()
    def _endPath(self): self.p.endPath()


def rect(x0, y0, x1, y1):
    p = Path()
    pen = p.getPen()
    pen.moveTo((x0, y0)); pen.lineTo((x1, y0)); pen.lineTo((x1, y1)); pen.lineTo((x0, y1)); pen.closePath()
    return p


def circle(cx, cy, r):
    k = 0.5523 * r
    p = Path()
    pen = p.getPen()
    pen.moveTo((cx + r, cy))
    pen.curveTo((cx + r, cy + k), (cx + k, cy + r), (cx, cy + r))
    pen.curveTo((cx - k, cy + r), (cx - r, cy + k), (cx - r, cy))
    pen.curveTo((cx - r, cy - k), (cx - k, cy - r), (cx, cy - r))
    pen.curveTo((cx + k, cy - r), (cx + r, cy - k), (cx + r, cy))
    pen.closePath()
    return p


def U(*ps):
    ps = [p for p in ps if len(list(p))]
    r = ps[0]
    for q in ps[1:]:
        r = op(r, q, PathOp.UNION)
    return r


def D(a, b): return op(a, b, PathOp.DIFFERENCE)
def I(a, b): return op(a, b, PathOp.INTERSECTION)


def xf(path, tf):
    out = Path()
    path.draw(TransformPen(ToPath(out), tf))
    return out


def svgd(path):
    pen = SVGPathPen(None, ntos=lambda v: f'{v:.2f}'.rstrip('0').rstrip('.'))
    path.draw(pen)
    return pen.getCommands()


def band(p, w):
    """Banda de ancho w centrada en el contorno de p."""
    s = Path(p)
    s.stroke(w, LineCap.ROUND_CAP, LineJoin.ROUND_JOIN, 4)
    s.convertConicsToQuads()
    return s


def grown(p, g): return U(p, band(p, 2 * g))
def inset(p, d): return D(p, band(p, 2 * d))


def clean(p, min_area=30):
    out = Path()
    for c in p.contours:
        if abs(c.area) >= min_area:
            out.addPath(c)
    return out


def font(ff): return TTFont(os.path.join(FONTS, ff))


def glyph(ff, name):
    f = font(ff)
    p = Path()
    dec = DecomposingRecordingPen(f.getGlyphSet())
    f.getGlyphSet()[name].draw(dec)
    dec.replay(ToPath(p))
    return p


# ---------- monograma ----------
UP, IT = 'Playfair_Display_wght_900.ttf', 'Playfair_Display_Italic_wght_900.ttf'
GAP = 4.0


def monogram(k_c=0.80, cx=136, cy=138):
    Ag, Cg = glyph(UP, 'A'), glyph(IT, 'C')
    ab = Ag.bounds
    sa = 216 / (ab[3] - ab[1])
    A = xf(Ag, (sa, 0, 0, -sa, 128 - sa * (ab[0] + ab[2]) / 2, 236 + sa * ab[1]))
    C = xf(Cg, (sa * k_c, 0, 0, -sa * k_c, 0, 0))
    b = C.bounds
    C = xf(C, (1, 0, 0, 1, cx - (b[0] + b[2]) / 2, cy - (b[1] + b[3]) / 2))
    xs = {}
    for y in range(20, 238, 2):
        segs = sorted((c.bounds for c in I(A, rect(-1e4, y - 0.25, 1e4, y + 0.25)).contours), key=lambda b: b[0])
        segs = [g for g in segs if g[2] - g[0] > 3]  # ignora las esquinas sueltas del ápice plano
        if len(segs) >= 2 and segs[1][0] - segs[0][2] > 1:
            xs[y] = (segs[0][2] + segs[1][0]) / 2
    ys = sorted(xs)
    for y in range(ys[0], ys[-1] + 1, 2):
        if y not in xs:
            lo, hi = max(k for k in ys if k < y), min(k for k in ys if k > y)
            xs[y] = xs[lo] + (xs[hi] - xs[lo]) * (y - lo) / (hi - lo)
    zone = Path()
    pen = zone.getPen()
    pen.moveTo((-1e4, ys[0]))
    for y, x in sorted(xs.items()):
        pen.lineTo((x, y))
    pen.lineTo((-1e4, 300))
    pen.closePath()
    X = I(A, C)
    yc = lambda q: (q.bounds[1] + q.bounds[3]) / 2
    hair = sorted([q for q in I(X, zone).contours if abs(q.area) > 2], key=yc)
    thick = sorted([q for q in D(X, zone).contours if abs(q.area) > 2], key=yc)
    # La pierna fina pasa siempre por delante de la C; en el trazo grueso, arriba pasa la C y abajo la A.
    mid_t = (yc(thick[0]) + yc(thick[-1])) / 2
    pieces = [(q, 'A', True) for q in hair] +              [(q, 'C' if (yc(q) < mid_t if len(thick) > 1 else yc(q) < cy) else 'A', False) for q in thick]
    A_hair, A_thick = I(A, zone), D(A, zone)
    A_cut, C_cut, shadows = Path(A), Path(C), []
    for q, top, is_hair in pieces:
        near = grown(q, GAP + 2)
        if top == 'C':   # la C delante del trazo grueso, sin tocar la pierna fina que va delante de ella
            over = I(C, near)
            A_cut = D(A_cut, D(grown(over, GAP), A_hair))
            shadows.append(('A', over))
        else:            # la pierna (fina o gruesa) delante de la C
            over = I(A_hair if is_hair else A_thick, near)
            C_cut = D(C_cut, grown(over, GAP))
            shadows.append(('C', over))
    return clean(A_cut), clean(C_cut), shadows, len(pieces)


def engrave(p, d=3.4, t=1.5):
    """Línea interior grabada: solo aparece donde el trazo es más grueso que 2(d+t)."""
    line = clean(D(inset(p, d), inset(p, d + t)), 4)
    return D(p, line) if len(list(line)) else p


# ---------- nombre trazado (kerning GPOS, tilde dorada) ----------
def kerning(f, glyphs):
    needed = set(zip(glyphs, glyphs[1:]))
    firsts, seconds = {a for a, _ in needed}, {b for _, b in needed}
    kern = {}
    for lookup in f['GPOS'].table.LookupList.Lookup:
        for st in lookup.SubTable:
            if lookup.LookupType == 9:
                st = st.ExtSubTable
            if getattr(st, 'LookupType', lookup.LookupType) != 2:
                continue
            if st.Format == 1:
                for i, a in enumerate(st.Coverage.glyphs):
                    for pvr in st.PairSet[i].PairValueRecord:
                        v = getattr(pvr.Value1, 'XAdvance', 0) if pvr.Value1 else 0
                        if v and (a, pvr.SecondGlyph) in needed:
                            kern.setdefault((a, pvr.SecondGlyph), v)
            elif st.Format == 2:
                c1, c2 = st.ClassDef1.classDefs, st.ClassDef2.classDefs
                for a in [g for g in st.Coverage.glyphs if g in firsts]:
                    rec = st.Class1Record[c1.get(a, 0)]
                    for b in seconds:
                        v = rec.Class2Record[c2.get(b, 0)].Value1
                        v = getattr(v, 'XAdvance', 0) if v else 0
                        if v and (a, b) in needed:
                            kern.setdefault((a, b), v)
    return kern


def word(text, ff, track=0.0):
    f = font(ff)
    gs, cmap, hmtx, upm = f.getGlyphSet(), f.getBestCmap(), f['hmtx'], f['head'].unitsPerEm
    glyphs = [cmap[ord(c)] for c in text]
    kern = kerning(f, glyphs)
    ink, gold, x = Path(), Path(), 0
    for i, g in enumerate(glyphs):
        if f['glyf'][g].isComposite():
            for comp in f['glyf'][g].components:
                dst = gold if 'acute' in comp.glyphName else ink
                dec = DecomposingRecordingPen(gs)
                gs[comp.glyphName].draw(dec)
                dec.replay(TransformPen(ToPath(dst), (1, 0, 0, 1, x + comp.x, comp.y)))
        else:
            gs[g].draw(TransformPen(ToPath(ink), (1, 0, 0, 1, x, 0)))
        x += hmtx[g][0] + track * upm + (kern.get((g, glyphs[i + 1]), 0) if i + 1 < len(glyphs) else 0)
    return ink, gold, x - track * upm, f['OS/2'].sCapHeight


# ---------- salida ----------
FOIL = ('<defs><linearGradient id="foil" gradientUnits="userSpaceOnUse" x1="40" y1="40" x2="216" y2="232">'
        '<stop offset="0" stop-color="#F6DA92"/><stop offset=".42" stop-color="#E3A93F"/>'
        '<stop offset=".7" stop-color="#C98E2B"/><stop offset="1" stop-color="#9A681C"/></linearGradient>'
        '<filter id="soft" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="3.2"/></filter></defs>')


def svg(view, body, title=NAME, defs=''):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{view}" role="img" aria-labelledby="t">'
            f'<title id="t">{title}</title>{defs}{body}</svg>\n')


def path(p, fill, extra=''):
    return f'<path fill="{fill}"{extra} d="{svgd(p)}"/>' if len(list(p)) else ''


def put(name, view, body, defs=''):
    open(os.path.join(OUT, name), 'w', encoding='utf-8').write(svg(view, body, defs=defs))


def emit(key, A, C, shadows, extra_ink=None):
    """Escribe símbolo plano, premium, en negro, en blanco, sobre oscuro, ícono de app y logos con nombre."""
    ink_all = U(A, extra_ink) if extra_ink is not None else A
    V = '0 0 256 256'
    put(f'{key}-color.svg', V, path(ink_all, INK) + path(C, GOLD))
    put(f'{key}-black.svg', V, path(U(ink_all, C), '#000000'))
    put(f'{key}-white.svg', V, path(U(ink_all, C), '#ffffff'))
    put(f'{key}-dark.svg', V, path(ink_all, CREAM) + path(C, GOLD))

    def premium(ink_fill):
        # Sombra suave del hilo de encima sobre el de abajo, recortada al hilo de abajo.
        sh = ''
        for i, (under, over) in enumerate(shadows):
            target = C if under == 'C' else ink_all
            cid = f'clip{key}{i}{ink_fill[1:]}'
            colour = GOLD_DEEP if under == 'C' else '#000000'
            sh += (f'<clipPath id="{cid}"><path d="{svgd(target)}"/></clipPath>'
                   f'<g clip-path="url(#{cid})"><path fill="{colour}" opacity="{".2" if under == "A" and ink_fill == CREAM else ".55"}" filter="url(#soft)" '
                   f'd="{svgd(grown(over, GAP + 1.5))}"/></g>')
        return path(ink_all, ink_fill) + path(C, 'url(#foil)') + sh

    put(f'{key}-premium.svg', V, premium(INK), FOIL)
    put(f'{key}-premium-dark.svg', V, premium(CREAM), FOIL)
    k = 0.70
    m = (k, 0, 0, k, 128 * (1 - k), 128 * (1 - k))
    put(f'{key}-app.svg', V, f'<rect width="256" height="256" rx="58" fill="{INK}"/>' + path(xf(ink_all, m), CREAM) + path(xf(C, m), 'url(#foil)'), FOIL)

    # Horizontal: monograma + nombre en Playfair SemiBold con tilde dorada.
    wi, wg, w, cap = word(NAME, 'Playfair_Display_wght_600.ttf', -0.005)
    H, CAP = 256, 256 * 0.40
    s = CAP / cap
    x0, base = 256 + 22, 128 + CAP / 2 + 8
    tf = (s, 0, 0, -s, x0, base)
    WI, WG = xf(wi, tf), xf(wg, tf)
    total = round(x0 + w * s + 8)
    hv = f'0 0 {total} {H}'
    put(f'{key}-lockup.svg', hv, path(ink_all, INK) + path(C, GOLD) + path(WI, INK) + path(WG, GOLD))
    put(f'{key}-lockup-dark.svg', hv, path(ink_all, CREAM) + path(C, GOLD) + path(WI, CREAM) + path(WG, GOLD))
    put(f'{key}-lockup-black.svg', hv, path(U(ink_all, C, WI, WG), '#000000'))
    put(f'{key}-lockup-white.svg', hv, path(U(ink_all, C, WI, WG), '#ffffff'))
    # Apilado: monograma arriba, nombre en versalitas espaciadas debajo.
    si, sg, sw, scap = word('AGORÁ CATALOG', 'Playfair_Display_wght_700.ttf', 0.16)
    SC = 34
    ss = SC / scap
    sx = 128 - sw * ss / 2
    stf = (ss, 0, 0, -ss, sx, 256 + 28 + SC)
    SI, SG = xf(si, stf), xf(sg, stf)
    left = min(0, sx - 8)
    sv = f'{left:.0f} 0 {256 - 2 * left:.0f} {256 + 28 + SC + 14}'
    put(f'{key}-stacked.svg', sv, path(ink_all, INK) + path(C, GOLD) + path(SI, INK) + path(SG, GOLD))
    put(f'{key}-stacked-dark.svg', sv, path(ink_all, CREAM) + path(C, 'url(#foil)') + path(SI, CREAM) + path(SG, GOLD), FOIL)
    put(f'{key}-stacked-premium.svg', sv, premium(INK) + path(SI, INK) + path(SG, GOLD), FOIL)


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    A, C, shadows, n = monogram()
    assert n >= 2, f'se esperaban al menos 2 cruces, hay {n}'
    print('cruces', n)
    # 1 Tejido
    emit('t', A, C, shadows)
    # 2 Grabado
    emit('g', engrave(A), engrave(C), shadows)
    # 3 Sello: aro fino detrás del monograma; la A lo rompe arriba y abajo.
    ring = D(circle(128, 138, 112), circle(128, 138, 108))
    ring = clean(D(ring, grown(U(A, C), GAP + 2)), 4)
    emit('s', A, C, shadows, extra_ink=ring)
    print('ok')
