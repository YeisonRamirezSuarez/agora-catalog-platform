# Arma docs/prototipo/v3.html desde index.html: catálogo rediseñado al boceto del cliente y panel con los campos nuevos.
# Uso: python -X utf8 build_v3.py <index.html> <v3.html>
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from v3_css import CAT_CSS, ADMIN_PREVIEW_OLD, ADMIN_PREVIEW_NEW, ADMIN_EXTRA, PANEL_BRAND  # noqa: E402
from v3_js import CAT_JS  # noqa: E402

src, dst = sys.argv[1], sys.argv[2]
s = open(src, encoding='utf-8').read()


def rep(old, new, count=1):
    global s
    n = s.count(old)
    assert n == count, (n, old[:120])
    s = s.replace(old, new)


def between(start, end, new):
    global s
    i, j = s.index(start), s.index(end)
    assert i < j and s.count(start) == 1, start
    old = s[i:j]
    s = s[:i] + new + s[j:]
    return old


# ---------- Cabecera y CSS ----------
rep('<link href="https://fonts.googleapis.com/css2?family=Onest:wght@400;500;600;700;800&display=swap" rel="stylesheet">',
    '<link href="https://fonts.googleapis.com/css2?family=Playfair+Display:ital,wght@0,500..800;1,500..800&family=Ms+Madi&family=Onest:wght@400;500;600;700;800&display=swap" rel="stylesheet">')
rep("  --ease:cubic-bezier(.16,1,.3,1);\n}",
    "  --ease:cubic-bezier(.16,1,.3,1);\n"
    "  /* Catálogo público: en :root porque las hojas se pintan en #modal-root, fuera de .store */\n"
    "  --night:#15120D;--cream:#F7F1E6;--sand:#EFE4D0;--paper:#FFFDF9;\n"
    "  --c-ink:#211C16;--c-ink-2:#4A4036;--c-muted:#6F655A;--c-line:#E7DDCC;--price:#B4182D;--on-accent:#231A0E;--accent:#DDB06C;\n"
    "  --serif:'Playfair Display',Georgia,'Times New Roman',serif;--script:'Ms Madi',cursive;\n}")
rep(".badge-out{display:inline-flex;align-items:center;height:22px;padding:0 8px;border-radius:6px;background:var(--ink);color:#fff;font-size:12px;font-weight:700}\n", '')
rep(".wa-mini{position:absolute;display:grid;place-items:center;width:36px;height:36px;border-radius:50%;background:#fff;color:var(--wa);box-shadow:var(--sh-2);text-decoration:none}\n"
    ".wa-mini:hover{background:var(--wa-soft)}\n.wa-mini svg{width:20px;height:20px}\n", '')
old_cat = between('/* ---------- Catálogo público ---------- */', '/* ---------- Login ---------- */', CAT_CSS)
search_css = '\n'.join(l for l in old_cat.splitlines() if l.startswith('.search'))  # el buscador del panel usa estas reglas
assert search_css.count('\n') == 4
rep('#toast.show{opacity:1;transform:translate(-50%,0)}\n', '#toast.show{opacity:1;transform:translate(-50%,0)}\n' + search_css + '\n')
rep(ADMIN_PREVIEW_OLD, ADMIN_PREVIEW_NEW)
rep('.crop-band{position:absolute;left:0;right:0;top:25.4%;bottom:25.4%;border-block:2px dashed rgba(255,255,255,.95);',
    '.crop-band{position:absolute;top:0;bottom:0;left:34.7%;right:34.7%;border-inline:2px dashed rgba(255,255,255,.95);')
rep('.preview b{font-weight:800}\n', '')
rep(".preview .pv-tab{display:inline-block;margin:0 12px 12px;padding-bottom:6px;border-bottom:3px solid var(--brand);font-weight:600;font-size:14px}\n", '')
rep('</style>', ADMIN_EXTRA + PANEL_BRAND + '</style>')

# ---------- noscript: páginas de la versión estática ----------
between('  <ul>\n    <li><a href="estatico/catalogo.html">', '  </ul>\n</div></noscript>',
        '  <ul>\n'
        '    <li><a href="estatico/catalogo.html">Inicio del catálogo</a>, <a href="estatico/catalogo-lista.html">catálogo con filtros</a>, '
        '<a href="estatico/catalogo-producto.html">producto abierto</a>, <a href="estatico/catalogo-pedido.html">mi pedido</a> y '
        '<a href="estatico/catalogo-whatsapp.html">elegir número de WhatsApp</a></li>\n'
        '    <li><a href="estatico/login.html">Ingreso</a>, <a href="estatico/panel.html">inicio del panel</a>, <a href="estatico/productos.html">productos</a>, '
        '<a href="estatico/producto.html">editar producto</a>, <a href="estatico/categorias.html">categorías</a>, '
        '<a href="estatico/colecciones.html">colecciones</a>, <a href="estatico/importar.html">importar</a></li>\n'
        '    <li><a href="estatico/configuracion.html">Configuración</a>, <a href="estatico/configuracion-recorte.html">recortar portada</a> y '
        '<a href="estatico/superadmin.html">super admin</a></li>\n')

# ---------- Íconos ----------
rep("  link: '<path d=\"M10 14a5 5 0 0 0 7 0l3-3a5 5 0 0 0-7-7l-1 1\"/><path d=\"M14 10a5 5 0 0 0-7 0l-3 3a5 5 0 0 0 7 7l1-1\"/>',\n",
    "  link: '<path d=\"M10 14a5 5 0 0 0 7 0l3-3a5 5 0 0 0-7-7l-1 1\"/><path d=\"M14 10a5 5 0 0 0-7 0l-3 3a5 5 0 0 0 7 7l1-1\"/>',\n" + r"""  menu: '<path d="M4 7h16M4 12h16M4 17h16"/>',
  cart: '<path d="M3 4h2.2l2.3 11.2a1 1 0 0 0 1 .8h8.9a1 1 0 0 0 1-.8L20 8H6.1"/><circle cx="9.5" cy="19.5" r="1.3"/><circle cx="17" cy="19.5" r="1.3"/>',
  arrow: '<path d="M4 12h16M14 6l6 6-6 6"/>',
  shield: '<path d="M12 3 5 6v5c0 4.5 3 8.3 7 10 4-1.7 7-5.5 7-10V6z"/><path d="m9 12 2 2 4-4"/>',
  award: '<circle cx="12" cy="9" r="6"/><path d="m8.5 14-1.5 7 5-3 5 3-1.5-7"/>',
  grid: '<rect x="4" y="4" width="7" height="7" rx="1"/><rect x="13" y="4" width="7" height="7" rx="1"/><rect x="4" y="13" width="7" height="7" rx="1"/><rect x="13" y="13" width="7" height="7" rx="1"/>',
  list: '<path d="M9 6h11M9 12h11M9 18h11"/><circle cx="4.5" cy="6" r="1"/><circle cx="4.5" cy="12" r="1"/><circle cx="4.5" cy="18" r="1"/>',
  filter: '<path d="M4 6h16M7 12h10M10 18h4"/>',
  trash: '<path d="M4 7h16M10 11v6M14 11v6"/><path d="M6 7l1 13h10l1-13M9 7V4h6v3"/>',
  layers: '<path d="m12 3 9 5-9 5-9-5z"/><path d="m3 13 9 5 9-5"/>',
  heart: '<path d="M12 20s-7.5-4.6-9.2-9.4C1.6 7.2 3.8 4 7.2 4c2 0 3.6 1.1 4.8 2.8C13.2 5.1 14.8 4 16.8 4c3.4 0 5.6 3.2 4.4 6.6C19.5 15.4 12 20 12 20z"/>',
  leafshield: '<path d="M12 3 5 6v5c0 4.5 3 8.3 7 10 4-1.7 7-5.5 7-10V6z"/><path d="M9 15c0-4 2.5-6 6-6 0 4-2.5 6-6 6zm0 0 3-3"/>',
  moto: '<circle cx="6" cy="17" r="2.6"/><circle cx="18" cy="17" r="2.6"/><path d="M8.6 17h6.8l2-6h-4l-2.4 3.5H6.5"/><path d="M14 7h2.5l1 4M3 10h6v3"/>',
""")

# ---------- Datos de ejemplo ----------
rep("    primario: '#2B4141', secundario: '#1B9C5F', precios: true, waProducto: true, agotados: true, activa: true,",
    "    primario: '#24331F', secundario: '#DDB06C', precios: true, pedidos: true, agotados: true, deco: true, fachada: 'salsamentaria', activa: true,")
rep("    direccion: 'Calle 45 # 12-30, barrio San José',", "    direccion: 'Calle 45 # 12-30',")
rep("    domicilios: 'Domicilio gratis en el barrio por compras desde $30.000.',", "    domicilios: 'Domicilio gratis por compras desde $30.000.',")
rep("  portada: { title: 'Ajustar portada', out: [1600, 754],", "  portada: { title: 'Ajustar portada', out: [3200, 1508],")  # nítida en pantallas retina
rep("    portada: 'img/portada.jpg', portadaSrc: 'img/portada.jpg', portadaCrop: null, admin: 'javier@ejemplo.com',\n",
    "    portada: 'img/portada.jpg', portadaSrc: 'img/portada.jpg', portadaCrop: null, admin: 'javier@ejemplo.com',\n" + r"""    corto: 'Javier', lema: 'Tradición y buen sabor',
    antetitulo: 'El sabor de reunirnos', titulo: 'Se ve rico.', tituloAcento: 'Sabe a\nun buen plan.',
    subtitulo: 'Embutidos, quesos y salsamentaria de la mejor calidad para tus momentos especiales.',
    nota: 'Productos\nque hacen\nmejores momentos', selloTexto: 'Tradición · desde siempre',
    sellos: [{ icono: 'calidad', texto: 'Productos frescos y de calidad' }, { icono: 'domicilio', texto: 'Entrega rápida a domicilio' },
      { icono: 'whatsapp', texto: 'Atención personalizada por WhatsApp' }, { icono: 'precio', texto: 'Precios justos para tu mesa' }],
    colTitulo: '¿Cuál es el plan?', colTexto: 'Elige tu momento y encuentra lo ideal para ti.', colNota: 'Siempre\nqueda bien.',
    catTitulo: 'Tradición\nen cada', catAcento: 'bocado.', catSubtitulo: 'Productos seleccionados de la mejor calidad para tus momentos especiales.',
    visitaNota: 'El buen sabor\nde siempre',
""")
rep("    { id: 5, nombre: 'Abarrotes', activa: true },\n",
    "    { id: 5, nombre: 'Abarrotes', activa: true },\n"
    "    { id: 6, nombre: 'Combos', activa: true, banner: { titulo: 'Combos\\nde la casa', frase: 'La combinación perfecta para compartir.', nota: 'Listos\\npara disfrutar', img: 'img/combos.jpg' } },\n")
rep("  disp: o.disp ?? true, dest: !!o.dest, img: `img/${codigo}-400.jpg`, big: `img/${codigo}.jpg`, photo: null, desc: o.desc ?? '',\n",
    "  disp: o.disp ?? true, dest: !!o.dest, img: o.img ?? `img/${codigo}-400.jpg`, big: o.img ?? `img/${codigo}.jpg`, photo: null, desc: o.desc ?? '',\n"
    "  creado: o.creado ?? shift(-120), // «Nuevo» en el catálogo durante 30 días\n")
rep("P('CF-003', 2, 'Jamón de pollo', 'Tajado x 250 g', 'UNIDAD', 9500, { desc:",
    "P('CF-003', 2, 'Jamón de pollo', 'Tajado x 250 g', 'UNIDAD', 9500, { creado: shift(-6), desc:")
rep("P('LAC-003', 4, 'Yogur de melocotón', 'Vaso x 200 g', 'UNIDAD', 2900, { desc:",
    "P('LAC-003', 4, 'Yogur de melocotón', 'Vaso x 200 g', 'UNIDAD', 2900, { creado: shift(-12), desc:")
rep("P('ABA-004', 5, 'Salsa de tomate', 'Frasco x 400 g', 'UNIDAD', 6200, { desc: 'Salsa de tomate para acompañar.' });\n",
    "P('ABA-004', 5, 'Salsa de tomate', 'Frasco x 400 g', 'UNIDAD', 6200, { desc: 'Salsa de tomate para acompañar.' });\n" + r"""P('CMB-001', 6, 'Combo picada para 4', 'Para 4 personas', 'UNIDAD', 46000, { creado: shift(-5), img: 'img/plan-picada.jpg', desc: 'Chorizo santarrosano, salchichón cervecero, queso costeño y arepas, listos para compartir.' });
P('CMB-002', 6, 'Combo desayuno', 'Para 2 personas', 'UNIDAD', 29500, { img: 'img/plan-desayuno.jpg', desc: 'Huevos, queso campesino, arepas, chorizo y kumis para un desayuno completo.' });
P('CMB-003', 6, 'Combo choripán', 'Para 6 personas', 'UNIDAD', 33900, { oferta: 29900, img: 'img/plan-antojo.jpg', desc: 'Chorizo santarrosano, pan y salsas para armar seis choripanes.' });
const idOf = codigo => S.prods.find(p => p.codigo === codigo).id;
// Colecciones: «¿Cuál es el plan?» en el inicio del catálogo.
S.cols = [
  { id: 1, nombre: 'Una buena picada', desc: 'Embutidos, quesos y conversación', img: 'img/plan-picada.jpg', visible: true, prods: ['EMB-001', 'EMB-002', 'QS-004', 'CF-001', 'CMB-001'] },
  { id: 2, nombre: 'Desayunar sin afán', desc: 'Los favoritos para empezar el día', img: 'img/plan-desayuno.jpg', visible: true, prods: ['ABA-001', 'ABA-002', 'QS-001', 'QS-003', 'LAC-001', 'EMB-003', 'CMB-002'] },
  { id: 3, nombre: 'Me dejo antojar', desc: 'Descubre todo el catálogo', img: 'img/plan-antojo.jpg', visible: true, prods: ['EMB-002', 'ABA-003', 'ABA-004', 'CF-004', 'CMB-003'] },
].map(c => ({ ...c, prods: c.prods.map(idOf) }));
""")
rep("const publicUrl = (e = S.empresa) => `http://186.147.6.105:2020/#/c/${path(e)}`;",
    "const publicUrl = (e = S.empresa) => `https://e-catalog-platform.duckdns.org:2020/#/c/${path(e)}`;")
between('const waAttrs = p =>', 'const visibles = () =>', r"""const MSG_GEN = 'Hola, vi su catálogo y quiero hacer un pedido.';
const msgProd = p => `Hola, vi en su catálogo: ${p.nombre} (${p.pres}). ¿Lo tiene disponible?`;
const waHref = (msg = MSG_GEN, numero = S.empresa.whatsapps[0]?.numero) => `https://wa.me/${numero}?text=${encodeURIComponent(msg)}`;
// Con varios números el clic abre primero el selector; data-msg lleva el mensaje ya escrito.
const waAttrs = msg => `href="${waHref(msg)}" target="_blank" rel="noopener"${S.empresa.whatsapps.length > 1 ? ` data-act="wa-pick"${msg ? ` data-msg="${esc(msg)}"` : ''}` : ''}`;
""")
rep("    f.elements[el.dataset.err]?.setAttribute('aria-invalid', m ? 'true' : 'false');",
    "    f.elements[el.dataset.err]?.setAttribute?.('aria-invalid', m ? 'true' : 'false'); // una lista de casillas no tiene setAttribute")

# ---------- Catálogo público (JS) ----------
between('// ---------- Catálogo público ----------', '// ---------- Login ----------', CAT_JS)

# ---------- Panel ----------
rep("['admin/categorias', 'Categorías', 'tag'], ['admin/importar', 'Importar', 'upload']",
    "['admin/categorias', 'Categorías', 'tag'], ['admin/colecciones', 'Colecciones', 'layers'], ['admin/importar', 'Importar', 'upload']")
rep("name=\"dest\"${p.dest ? ' checked' : ''}> En «Los más pedidos»</label>",
    "name=\"dest\"${p.dest ? ' checked' : ''}> Marcar como «Más vendido»</label>")
rep("  else S.prods.push({ id: ++pid, img: null, big: null, photo: null, ...data });",
    "  else S.prods.push({ id: ++pid, img: null, big: null, photo: null, creado: today(), ...data });")

# Categorías: franja en el inicio
rep("""    <span class="grow"><input class="cat-name" data-catname="${c.id}" value="${esc(c.nombre)}" aria-label="Nombre de la categoría"></span>""",
    """    <span class="grow"><input class="cat-name" data-catname="${c.id}" value="${esc(c.nombre)}" aria-label="Nombre de la categoría">${c.banner ? '<span class="chip-on">Franja en el inicio</span>' : ''}</span>""")
rep("""    <button class="btn icon" type="button" data-act="del-cat" data-id="${c.id}" aria-label="Eliminar ${esc(c.nombre)}">${ic('x')}</button>""",
    """    <button class="btn icon" type="button" data-act="banner" data-id="${c.id}" aria-label="Franja de ${esc(c.nombre)} en el inicio" title="Franja en el inicio">${ic('photo')}</button>
    <button class="btn icon" type="button" data-act="del-cat" data-id="${c.id}" aria-label="Eliminar ${esc(c.nombre)}">${ic('x')}</button>""")
rep("<p>Arrastra para cambiar el orden en que aparecen en tu catálogo.</p>",
    "<p>Arrastra para cambiar el orden en que aparecen en tu catálogo. Con el botón de foto, una categoría sale como franja grande en el inicio.</p>")
rep("""  S.cats.splice(to, 0, S.cats.splice(from, 1)[0]);
  return true;
}
""", r"""  S.cats.splice(to, 0, S.cats.splice(from, 1)[0]);
  return true;
}
function bannerSheet(id) {
  const c = S.cats.find(x => x.id === id), b = c.banner || { titulo: c.nombre, frase: '', nota: '', img: '' };
  const other = S.cats.find(x => x.banner && x.id !== id);
  pendingPhoto = null;
  sheet(`<form class="sheet-scroll form-stack" data-form="banner" data-id="${id}" novalidate><h2>Franja de ${esc(c.nombre)}</h2>
    <p class="muted">Una franja grande con foto en el inicio de tu catálogo, con un botón que lleva a esta categoría. Solo una categoría puede tenerla.</p>
    ${tog('on', 'Mostrar la franja en el inicio', other ? `Se quita la de ${esc(other.nombre)}.` : 'Si la apagas, no sale en el inicio.', !!c.banner)}
    ${fld('titulo', 'Título', area('titulo', b.titulo, 40), { hint: 'Con Enter partes la línea.' })}
    ${fld('frase', 'Frase', inp('frase', b.frase, 'maxlength="80"'), { hint: 'Opcional. Una línea corta.' })}
    ${fld('nota', 'Nota escrita a mano', area('nota', b.nota, 32), { hint: 'Opcional. Sale en una etiqueta sobre la foto.' })}
    <div class="field banner-prev"><span class="flabel">Foto</span>
      <div id="photo-prev">${b.img ? `<span class="ph"><img src="${b.img}" alt=""></span>` : `<span class="ph"><span class="ph-none">${ic('photo')}</span></span>`}</div>
      <label class="drop">${ic('photo')}<span><b>${b.img ? 'Cambiar foto' : 'Subir foto'}</b><br>Horizontal, de al menos 1600 px de ancho. Hasta 5 MB.</span><input type="file" class="sr" id="photo" accept="image/jpeg,image/png,image/webp"></label>
      <p class="err" id="photo-err" data-err="img" aria-live="polite"></p></div>
    <div class="form-actions"><button type="button" class="btn" data-act="close">Cancelar</button><button class="btn btn-dark">Guardar franja</button></div>
  </form>`, `Franja de ${c.nombre}`);
}
function submitBanner(f) {
  const v = Object.fromEntries(new FormData(f)), c = S.cats.find(x => x.id === +f.dataset.id), err = {};
  const img = pendingPhoto || c.banner?.img || '';
  if (v.on && !tx(v.titulo)) err.titulo = 'Escribe el título de la franja.';
  if (v.on && !img) err.img = 'Sube una foto para la franja.';
  if (showErr(f, err)) return;
  if (v.on) S.cats.forEach(x => { if (x !== c) x.banner = null; });
  c.banner = v.on ? { titulo: tx(v.titulo), frase: v.frase.trim(), nota: tx(v.nota), img } : null;
  pendingPhoto = null;
  closeModal();
  categorias();
  toast(v.on ? `Franja de ${c.nombre} en el inicio` : `Franja de ${c.nombre} quitada`);
}
""")

# Colecciones
rep("// ---------- Panel: importar ----------", r"""// ---------- Panel: colecciones ----------
const MAX_COLS = 6;
function colecciones() {
  const e = S.empresa;
  shell('admin/colecciones', `<header class="page-head"><div><h1>Colecciones</h1><p>Grupos de productos para un momento, como una picada o el desayuno. Salen en el inicio de tu catálogo.</p></div>
      ${S.cols.length < MAX_COLS ? `<a class="btn btn-dark" href="#/admin/coleccion/nueva">${ic('plus')}Nueva colección</a>` : ''}</header>
    <form class="panel form" data-form="colsec" novalidate><h2>Título de la sección</h2>
      ${fld('colTitulo', 'Título', inp('colTitulo', e.colTitulo, 'required maxlength="40"'))}
      ${fld('colTexto', 'Texto', inp('colTexto', e.colTexto, 'maxlength="80"'), { hint: 'Opcional.' })}
      ${fld('colNota', 'Nota escrita a mano', area('colNota', e.colNota, 40), { full: true, hint: 'Opcional. Sale en computador, junto al título. Con Enter partes la línea.' })}
      <div class="form-actions full"><button class="btn btn-dark">Guardar título</button></div></form>
    ${S.cols.length ? `<ul class="col-list">${S.cols.map(c => `<li><span class="ph cat-thumb">${c.img ? `<img src="${c.img}" alt="">` : `<span class="ph-none">${ic('layers')}</span>`}</span>
      <span class="grow"><b>${esc(c.nombre)}</b><span class="muted">${plural(c.prods.length, 'producto')}</span></span>
      <label class="sw-label"><input type="checkbox" class="switch" role="switch" data-colvis="${c.id}"${c.visible ? ' checked' : ''} aria-label="${esc(c.nombre)} visible en el catálogo"><span>Visible</span></label>
      <a class="btn icon" href="#/admin/coleccion/${c.id}" aria-label="Editar ${esc(c.nombre)}">${ic('edit')}</a></li>`).join('')}</ul>`
    : `<div class="panel empty"><p>Todavía no tienes colecciones. Sin colecciones, el inicio de tu catálogo no muestra «${esc(e.colTitulo)}».</p></div>`}
    <p class="muted note">Máximo ${MAX_COLS} colecciones.</p>`);
}
function submitColSec(f) {
  const v = Object.fromEntries(new FormData(f));
  if (showErr(f, v.colTitulo.trim() ? {} : { colTitulo: 'Escribe el título de la sección.' })) return;
  Object.assign(S.empresa, { colTitulo: v.colTitulo.trim(), colTexto: v.colTexto.trim(), colNota: tx(v.colNota) });
  toast('Título guardado');
}
function coleccionForm(id) {
  const c = id === 'nueva' ? { id: null, nombre: '', desc: '', img: null, visible: true, prods: [] } : S.cols.find(x => x.id === +id);
  if (!c || (!c.id && S.cols.length >= MAX_COLS)) return colecciones();
  pendingPhoto = null;
  shell('admin/colecciones', `<header class="page-head"><div><a class="back" href="#/admin/colecciones">${ic('back')}Colecciones</a><h1>${c.id ? esc(c.nombre) : 'Nueva colección'}</h1></div></header>
  <form class="two-col" data-form="col" data-id="${c.id ?? ''}" novalidate>
    <div class="panel form">
      ${fld('nombre', 'Nombre', inp('nombre', c.nombre, 'required maxlength="40" placeholder="Ej.: Para una picada"'), { full: true })}
      ${fld('desc', 'Descripción', inp('desc', c.desc, 'maxlength="80"'), { full: true, hint: 'Opcional. Una línea corta.' })}
      <div class="field full"><span class="flabel" id="pick-label">Productos</span>
        <label class="search pick-search">${ic('search')}<input type="search" id="pick-q" placeholder="Buscar por nombre o código" aria-label="Buscar productos para la colección"></label>
        <ul class="pick-list" aria-labelledby="pick-label">${S.prods.map(p => `<li data-pick="${esc(norm(`${p.nombre} ${p.codigo}`))}"><label><input type="checkbox" name="prods" value="${p.id}"${c.prods.includes(p.id) ? ' checked' : ''}>${thumb(p)}<span><b>${esc(p.nombre)}</b><span class="muted">${esc(p.pres)}</span></span></label></li>`).join('')}</ul>
        <p class="err" data-err="prods" aria-live="polite"></p></div>
      <div class="field full toggles"><label class="sw-label"><input type="checkbox" class="switch" role="switch" name="visible"${c.visible ? ' checked' : ''}> Visible en el catálogo</label></div>
    </div>
    <div class="panel photo-panel"><h2>Foto</h2>
      <div id="photo-prev"><span class="ph">${c.img ? `<img src="${c.img}" alt="">` : `<span class="ph-none">${ic('photo')}</span>`}</span></div>
      <label class="drop">${ic('photo')}<span><b>${c.img ? 'Cambiar foto' : 'Subir foto'}</b><br>JPG, PNG o WebP. Hasta 5 MB.</span><input type="file" class="sr" id="photo" accept="image/jpeg,image/png,image/webp"></label>
      <p class="err" id="photo-err" aria-live="polite"></p>
      <div class="form-actions">${c.id ? `<button type="button" class="btn" data-act="del-col" data-id="${c.id}">Eliminar</button>` : ''}<a class="btn" href="#/admin/colecciones">Cancelar</a><button class="btn btn-dark">${c.id ? 'Guardar cambios' : 'Crear colección'}</button></div>
    </div>
  </form>`);
}
function submitCol(f) {
  const fd = new FormData(f), id = +f.dataset.id || null, err = {}, nombre = fd.get('nombre').trim(), prods = fd.getAll('prods').map(Number);
  if (!nombre) err.nombre = 'Escribe el nombre de la colección.';
  if (!prods.length) err.prods = 'Elige al menos un producto.';
  if (showErr(f, err)) return;
  const data = { nombre, desc: fd.get('desc').trim(), visible: !!fd.get('visible'), prods };
  if (pendingPhoto) data.img = pendingPhoto;
  if (id) Object.assign(S.cols.find(c => c.id === id), data);
  else S.cols.push({ id: Math.max(0, ...S.cols.map(c => c.id)) + 1, img: null, ...data });
  pendingPhoto = null;
  toast(id ? 'Cambios guardados' : 'Colección creada');
  location.hash = '#/admin/colecciones';
}

// ---------- Panel: importar ----------""")

# Configuración
between('function previewMedia() {', '// Editor de recorte:', r"""const pvTitle = () => {
  const e = S.empresa, f = $('form[data-form="conf"]');
  const t = f ? f.elements.titulo.value.trim() : e.titulo, a = f ? f.elements.tituloAcento.value.trim() : e.tituloAcento;
  return `${br(t || e.nombre)}${a ? ` <em>${br(a)}</em>` : ''}`;
};
function previewMedia() {
  const e = S.empresa, cover = media('portada'), lg = media('logo');
  const box = lg ? `<span class="logo has-img" aria-hidden="true"><img src="${lg.url}" alt=""></span>` : logo({ nombre: e.nombre });
  return `<button type="button" class="pv-cover" data-act="edit-media" data-kind="portada" aria-label="${cover ? 'Cambiar o ajustar la portada' : 'Subir portada'}">
      ${cover ? `<img src="${cover.url}" alt="">` : '<span class="pv-empty">Subir portada</span>'}
      <span class="pv-txt" aria-hidden="true"><span class="pv-t" id="pv-t">${pvTitle()}</span><span class="pv-btn">Explorar el catálogo</span></span><span class="pv-cam">${ic('camera')}</span></button>
    <button type="button" class="pv-logo" data-act="edit-media" data-kind="logo" aria-label="${lg ? 'Cambiar o ajustar el logo' : 'Subir logo'}">${box}<span class="pv-cam">${ic('camera')}</span></button>`;
}

""")
rep("hint: 'Arrastra la foto para encuadrarla. En el celular se ve completa; en computador, la franja entre las líneas.' },",
    "hint: 'Arrastra la foto para encuadrarla. En computador se ve completa; en el celular, la franja entre las líneas, detrás del texto.' },")
rep("""const contrastWithWhite = hex => 1.05 / (lum(hex) + 0.05);
console.assert(contrastWithWhite('#1E3B34') > 4.5 && contrastWithWhite('#FFFF00') < 4.5, 'contraste');""",
    r"""const contrast = (a, b) => { const [x, y] = [lum(a), lum(b)].sort((m, n) => n - m); return (x + 0.05) / (y + 0.05); };
const contrastWithWhite = hex => contrast(hex, '#FFFFFF');
const colorErr = (k, hex) => k === 'primario'
  ? (contrastWithWhite(hex) < 4.5 ? 'El texto blanco casi no se lee sobre este color. Elige un tono más oscuro.' : '')
  : (contrast(hex, NIGHT) < 4.5 ? 'Este color casi no se lee sobre la portada oscura. Elige un tono más claro.' : '');
console.assert(contrastWithWhite('#1E3B34') > 4.5 && contrastWithWhite('#FFFF00') < 4.5 && !colorErr('secundario', '#DDB06C') && colorErr('secundario', '#24331F'), 'contraste');
const SELLO_OPC = { calidad: 'Calidad', domicilio: 'Domicilio', whatsapp: 'WhatsApp', precio: 'Precio', tradicion: 'Tradición', horario: 'Horario' };
const selloRow = (s, i) => `<div class="sello-row"><select class="input" name="sello_icono" aria-label="Ícono del sello ${i + 1}">${Object.entries(SELLO_OPC).map(([k, t]) => `<option value="${k}"${k === s.icono ? ' selected' : ''}>${t}</option>`).join('')}</select>`
  + `<input class="input" name="sello_texto" value="${esc(s.texto)}" maxlength="48" aria-label="Texto del sello ${i + 1}"></div>`;""")
rep("""function configuracion() {
  const e = S.empresa;
  pend = {};""", """const area = (name, val, max) => `<textarea class="input" id="f-${name}" name="${name}" rows="2" maxlength="${max}">${esc(val ?? '')}</textarea>`;
const tx = s => s.replace(/\\r\\n?/g, '\\n').trim(); // los textarea pueden llegar con CRLF
function configuracion() {
  const e = S.empresa, sellos = [...e.sellos, ...Array(4).fill({ icono: 'calidad', texto: '' })].slice(0, 4);
  pend = {};""")
rep("""        ${fld('nombre', 'Nombre', inp('nombre', e.nombre, 'required'), { full: true })}
        ${fld('slug',""", """        ${fld('nombre', 'Nombre', inp('nombre', e.nombre, 'required'), { full: true })}
        ${fld('corto', 'Nombre corto', inp('corto', e.corto, 'maxlength="24"'), { hint: 'El final del nombre que sale en grande. Ej.: Javier.' })}
        ${fld('lema', 'Lema', inp('lema', e.lema, 'maxlength="32"'), { hint: 'Opcional. Debajo del nombre, en el encabezado.' })}
        ${fld('slug',""")
rep("""      <section class="panel"><h2>Qué se muestra</h2>
        ${tog('precios', 'Mostrar precios', 'Si lo apagas, el catálogo no muestra precios ni ofertas.', e.precios)}
        ${tog('waProducto', 'Botón de WhatsApp en cada producto', 'Tu cliente te escribe con el nombre del producto ya puesto.', e.waProducto)}""",
    r"""      <section class="panel form"><h2>Portada del catálogo</h2>
        ${fld('antetitulo', 'Antetítulo', inp('antetitulo', e.antetitulo, 'maxlength="40"'), { full: true, hint: 'Opcional. Línea corta en mayúsculas encima del titular.' })}
        ${fld('titulo', 'Titular', inp('titulo', e.titulo, 'maxlength="40"'), { full: true, hint: 'Sin titular, la portada muestra el nombre del negocio.' })}
        ${fld('tituloAcento', 'Segunda parte, en cursiva', area('tituloAcento', e.tituloAcento, 40), { full: true, hint: 'Opcional. Sale en el color de acento. Con Enter partes la línea.' })}
        ${fld('subtitulo', 'Texto debajo del titular', `<textarea class="input" id="f-subtitulo" name="subtitulo" rows="2" maxlength="140">${esc(e.subtitulo)}</textarea>`, { full: true })}
        ${fld('selloTexto', 'Texto del sello redondo', inp('selloTexto', e.selloTexto, 'maxlength="30"'), { full: true, hint: 'Sale en computador alrededor de la inicial. Sin texto, va el nombre del negocio.' })}
      </section>
      <section class="panel form"><h2>Página de catálogo</h2>
        ${fld('catTitulo', 'Titular', area('catTitulo', e.catTitulo, 40), { full: true, hint: 'Sin titular, la página dice «Nuestro catálogo».' })}
        ${fld('catAcento', 'Final en cursiva', inp('catAcento', e.catAcento, 'maxlength="30"'), { full: true })}
        ${fld('catSubtitulo', 'Texto debajo', inp('catSubtitulo', e.catSubtitulo, 'maxlength="140"'), { full: true })}
      </section>
      <section class="panel form"><h2>Notas escritas a mano</h2>
        ${fld('nota', 'Junto al titular de la portada', area('nota', e.nota, 48), { full: true, hint: 'Sale en computador. Con Enter partes la línea.' })}
        ${fld('visitaNota', 'En «Nos vemos en…»', area('visitaNota', e.visitaNota, 40), { full: true })}
      </section>
      <section class="panel"><h2>Dibujo de «Nos vemos en…»</h2><p class="muted">Sale arriba de los datos de contacto, con el nombre de tu negocio en el letrero. Elige el que más se parezca a tu local.</p>
        <div class="shop-pick" role="radiogroup" aria-label="Dibujo de la sección de contacto">${[...Object.keys(SHOPS), ''].map(k => `<label class="shop-opt"><input type="radio" name="fachada" value="${k}"${(SHOPS[e.fachada] ? e.fachada : '') === k ? ' checked' : ''}>${k ? shopArt(e, k) : '<span class="shop-none">Sin dibujo</span>'}<span>${k ? SHOPS[k].label : 'Ninguno'}</span></label>`).join('')}</div></section>
      <section class="panel"><h2>Sellos</h2><p class="muted">Frases cortas que salen debajo de la portada, cada una con su ícono. Deja en blanco las que no uses.</p>
        <div class="sellos-ed">${sellos.map(selloRow).join('')}</div></section>
      <section class="panel"><h2>Qué se muestra</h2>
        ${tog('precios', 'Mostrar precios', 'Si lo apagas, el catálogo no muestra precios ni ofertas.', e.precios)}
        ${tog('deco', 'Adornos de hojas en los bordes', 'Hojas de laurel y perejil junto a los títulos, en computador.', e.deco)}
        ${tog('pedidos', 'Pedido por WhatsApp', 'Tus clientes suman productos a un pedido y te lo envían por WhatsApp con cantidades y total. Si lo apagas, cada producto tiene un botón para preguntar.', e.pedidos)}""")
rep("""      <div class="preview" id="cprev" style="--brand:${e.primario};--brand-2:${e.secundario}">
        <div id="prev-media">${previewMedia()}</div>
        <span class="pv-tab">Ofertas de hoy</span>
      </div>""", """      <div class="pv" id="cprev" style="--brand:${e.primario};--accent:${e.secundario}">
        <div id="prev-media">${previewMedia()}</div>
        <div class="pv-buy" aria-hidden="true"><span>Botón para agregar al pedido</span><span class="pv-add">${ic('cart')}</span></div>
      </div>""")
rep("""        ${fld('primario', 'Color principal', `<input type="color" class="color" id="f-primario" name="primario" value="${e.primario}">`)}
        ${fld('secundario', 'Color secundario', `<input type="color" class="color" id="f-secundario" name="secundario" value="${e.secundario}">`)}
      </div>
      <p class="err" id="contrast" aria-live="polite"></p>""",
    """        ${fld('primario', 'Color de botones', `<input type="color" class="color" id="f-primario" name="primario" value="${e.primario}">`, { hint: 'Oscuro, para que el texto blanco se lea.' })}
        ${fld('secundario', 'Color de acento', `<input type="color" class="color" id="f-secundario" name="secundario" value="${e.secundario}">`, { hint: 'Claro, para que se lea sobre la portada oscura.' })}
      </div>""")
rep("""  $$('[name=wa_numero]', f).forEach(i => i.setAttribute('aria-invalid', String(!!bad && i.value.trim() === bad.numero)));
  if (showErr(f, err)) return;
  Object.assign(S.empresa, {
    nombre: v.nombre.trim(), slug: v.slug, whatsapps, direccion: v.direccion.trim(), horario: v.horario.trim(), domicilios: v.domicilios.trim(),
    primario: v.primario, secundario: v.secundario, precios: !!v.precios, waProducto: !!v.waProducto, agotados: !!v.agotados,
  });""", """  $$('[name=wa_numero]', f).forEach(i => i.setAttribute('aria-invalid', String(!!bad && i.value.trim() === bad.numero)));
  for (const k of ['primario', 'secundario']) if (colorErr(k, v[k])) err[k] = colorErr(k, v[k]);
  if (showErr(f, err)) return;
  const textos = new FormData(f).getAll('sello_texto');
  Object.assign(S.empresa, {
    nombre: v.nombre.trim(), slug: v.slug, whatsapps, direccion: v.direccion.trim(), horario: v.horario.trim(), domicilios: v.domicilios.trim(),
    primario: v.primario, secundario: v.secundario, precios: !!v.precios, pedidos: !!v.pedidos, agotados: !!v.agotados, deco: !!v.deco, fachada: SHOPS[v.fachada] ? v.fachada : '',
    corto: v.corto.trim(), lema: v.lema.trim(), antetitulo: v.antetitulo.trim(), titulo: v.titulo.trim(), tituloAcento: tx(v.tituloAcento),
    subtitulo: v.subtitulo.trim(), selloTexto: v.selloTexto.trim(), nota: tx(v.nota), visitaNota: tx(v.visitaNota),
    catTitulo: tx(v.catTitulo), catAcento: v.catAcento.trim(), catSubtitulo: v.catSubtitulo.trim(),
    sellos: new FormData(f).getAll('sello_icono').map((icono, i) => ({ icono, texto: textos[i].trim() })).filter(x => x.texto),
  });""")

# ---------- Eventos ----------
rep("  info: openInfo,\n", '')  # Contáctanos ya no abre una hoja: lleva a la sección de contacto
rep("""  find: () => { goTo($('#results .sec')?.id || 'results'); $('#q').focus({ preventScroll: true }); },
  tab: t => goTo(t.dataset.to),
""", r"""  menu: openMenu,
  order: openOrder,
  filters: openFilters,
  find: () => {
    if ($('#cq')) return $('#cq').focus();
    focusSearch = true;
    location.hash = `#/c/${path(S.empresa)}/catalogo`;
  },
  qty: t => {
    const box = t.closest('.qty'), out = $('output', box), n = Math.min(99, Math.max(1, +out.textContent + +t.dataset.d));
    out.textContent = n;
    $('[data-d="-1"]', box).setAttribute('aria-disabled', String(n <= 1));
    $('[data-d="1"]', box).setAttribute('aria-disabled', String(n >= 99));
  },
  add: t => {
    const p = byId(+t.dataset.id), out = $('.qty output', t.parentElement), n = +out.textContent;
    setQty(p.id, (cart[p.id] || 0) + n);
    if (t.dataset.close) closeModal();
    else { out.textContent = 1; $('.qty [data-d="-1"]', t.parentElement).setAttribute('aria-disabled', 'true'); }
    paintCart(true);
    toast(`Agregado a tu pedido: ${n} × ${p.nombre}`);
  },
  line: t => { const id = +t.dataset.id; setQty(id, (cart[id] || 0) + +t.dataset.d); paintCart(); },
  empty: () => { cart = {}; saveCart(); paintCart(); toast('Tu pedido quedó vacío'); },
  fav: t => {
    FAV.tab = t.dataset.k;
    $$('[data-act="fav"]').forEach(b => b.setAttribute('aria-pressed', String(b === t)));
    $('#fv-row').innerHTML = favList(FAV.tab).slice(0, 12).map(pcard).join('');
    $('#fv-row').scrollLeft = 0;
    $('#fv-all').href = favHref(FAV.tab);
  },
  rail: t => { const r = $('#fv-row'); r.scrollBy({ left: +t.dataset.d * r.clientWidth * 0.8, behavior: motion() }); },
  vista: t => {
    FIL.lista = t.dataset.v === 'lista';
    $$('[data-act="vista"]').forEach(b => b.setAttribute('aria-pressed', String(b === t)));
    paintResults();
  },
  unfil: t => {
    const k = t.dataset.k;
    if (k.startsWith('cat:')) FIL.cats.delete(+k.slice(4));
    else if (k === 'precio') FIL.min = FIL.max = 0;
    else FIL[k] = k === 'col' ? 0 : false;
    refreshFilters();
    paintResults();
    ($('.fchip') || $('#cq')).focus();
  },
  'clear-fil': () => {
    Object.assign(FIL, { cats: new Set(), promo: false, mas: false, disp: false, favs: false, col: 0, q: '', min: 0, max: 0 });
    $('#cq').value = '';
    refreshFilters();
    paintResults();
    $('#cq').focus();
  },
  heart: t => {
    const id = +t.dataset.id, on = !favs.has(id);
    if (on) favs.add(id); else favs.delete(id);
    save('favoritos', [...favs]);
    $$(`[data-act="heart"][data-id="${id}"]`).forEach(b => b.setAttribute('aria-pressed', String(on)));
    toast(on ? 'Guardado en tus favoritos' : 'Quitado de tus favoritos');
    if (!$('#results')) return;
    if (!favs.size) FIL.favs = false;
    refreshFilters();
    if (FIL.favs || !favs.size) paintResults();
  },
  banner: t => bannerSheet(+t.dataset.id),
  'del-col': t => {
    const c = S.cols.find(x => x.id === +t.dataset.id);
    S.cols = S.cols.filter(x => x !== c);
    toast(`Colección ${c.nombre} eliminada`);
    location.hash = '#/admin/colecciones';
  },
""")
rep("  ({ login: submitLogin, prod: submitProd, cat: submitCat, conf: submitConf, emp: submitEmp })[ev.target.dataset.form]?.(ev.target);",
    """  ({
    login: submitLogin, prod: submitProd, cat: submitCat, conf: submitConf, emp: submitEmp, banner: submitBanner, col: submitCol, colsec: submitColSec,
    order: submitOrder, csearch: () => $('#cq').blur(),
    find: f => { const q = f.elements.q.value.trim(); location.hash = `#/c/${path(S.empresa)}/catalogo${q ? `?q=${encodeURIComponent(q)}` : ''}`; },
  })[ev.target.dataset.form]?.(ev.target);""")
rep("""  if (t.id === 'q') {
    const q = t.value.trim();
    $('#results').innerHTML = sections(q);
    $('#tabs').hidden = !!q;
    if (!q) { $('#tabs').innerHTML = tabsFor(); startSpy(); }
  } else if (t.id === 'crop-z') {""", r"""  if (t.id === 'cq') {
    FIL.q = t.value.trim();
    paintResults();
  } else if ('fmin' in t.dataset || 'fmax' in t.dataset) {
    const g = t.closest('.f-group'), [a, b] = $$('input[type=range]', g), [lo, hi] = bounds();
    if (+a.value > +b.value) t.value = t === a ? b.value : a.value;
    FIL.min = +a.value > lo ? +a.value : 0;
    FIL.max = +b.value < hi ? +b.value : 0;
    $('.range-txt', g).textContent = `${cop(+a.value)} - ${cop(+b.value)}`;
    paintResults();
  } else if (t.id === 'pick-q') {
    const q = norm(t.value.trim());
    $$('[data-pick]').forEach(li => { li.hidden = !li.dataset.pick.includes(q); });
  } else if (t.id === 'crop-z') {""")
rep("""  } else if (t.type === 'color') {
    $('#cprev').style.setProperty(t.name === 'primario' ? '--brand' : '--brand-2', t.value);
    const bad = contrastWithWhite($('#f-primario').value) < 4.5;
    $('#contrast').textContent = bad ? 'El texto blanco casi no se lee sobre este color principal. Elige un tono más oscuro.' : '';
  } else if (form === 'conf' && t.name === 'slug') {""", """  } else if (t.type === 'color') {
    $('#cprev').style.setProperty(t.name === 'primario' ? '--brand' : '--accent', t.value);
    $(`[data-err="${t.name}"]`).textContent = colorErr(t.name, t.value);
  } else if (form === 'conf' && (t.name === 'titulo' || t.name === 'tituloAcento')) {
    $('#pv-t').innerHTML = pvTitle();
  } else if (form === 'conf' && t.name === 'slug') {""")
rep("""  } else if (t.id === 'photo' && t.files[0]) {""", """  } else if (t.dataset.fcat) {
    const id = +t.dataset.fcat;
    if (t.checked) FIL.cats.add(id); else FIL.cats.delete(id);
    syncFilters(t);
    paintResults();
  } else if (t.dataset.fflag) {
    FIL[t.dataset.fflag] = t.checked;
    syncFilters(t);
    paintResults();
  } else if (t.id === 'corden') {
    FIL.sort = t.value;
    paintResults();
  } else if (t.dataset.colvis) {
    const c = S.cols.find(x => x.id === +t.dataset.colvis);
    c.visible = t.checked;
    toast(`${c.nombre}: ${c.visible ? 'visible' : 'oculta'} en el catálogo`);
  } else if (t.id === 'photo' && t.files[0]) {""")

# ---------- Rutas ----------
rep("""const routes = {
  c: catalogo, login, admin: dashboard, 'admin/productos': productos, 'admin/producto': productoForm,
  'admin/categorias': categorias, 'admin/importar': importar, 'admin/configuracion': configuracion, superadmin,
};
function render() {
  closeModal();
  spy?.disconnect();
  const parts = (location.hash.slice(2) || `c/${path(S.empresa)}`).split('/');
  let key = parts.join('/'), arg;
  if (!routes[key]) { arg = parts.pop(); key = parts.join('/'); }
  (routes[key] || noDisponible)(arg);
  window.scrollTo(0, 0);
}""", """const routes = {
  login, admin: dashboard, 'admin/productos': productos, 'admin/producto': productoForm, 'admin/categorias': categorias,
  'admin/colecciones': colecciones, 'admin/coleccion': coleccionForm, 'admin/importar': importar, 'admin/configuracion': configuracion, superadmin,
};
function render() {
  closeModal();
  $('meta[name="theme-color"]').content = '#FFFFFF';
  const [h, qs = ''] = location.hash.slice(2).split('?');
  const parts = (h || `c/${path(S.empresa)}`).split('/');
  document.body.dataset.surface = parts[0] === 'c' ? 'store' : 'panel'; // el panel y el login llevan la marca Agorá
  if (parts[0] === 'c') catalogo(parts[1], parts.slice(2), qs);
  else {
    let key = parts.join('/'), arg;
    if (!routes[key]) { arg = parts.pop(); key = parts.join('/'); }
    (routes[key] || noDisponible)(arg);
  }
  window.scrollTo(0, 0);
}""")
rep("  document.documentElement.style.setProperty('--brand', e.primario);",
    "  $('meta[name=\"theme-color\"]').content = NIGHT;\n  document.documentElement.style.setProperty('--brand', e.primario);")

# Restos del catálogo viejo que no deben quedar
for gone in ('waProducto', 'waLink', 'startSpy', 'goTo(', 'tabsFor', 'sections(', 'brand-2:${e.secundario}', "'#q'", '.preview', 'pv-tab', 'id="contrast"',
             'openInfo', 'bbar', 'Bodoni', 'Belle Aurore'):
    assert gone not in s, gone
open(dst, 'w', encoding='utf-8', newline='\n').write(s)
print('ok', dst, len(s.splitlines()), 'líneas')
