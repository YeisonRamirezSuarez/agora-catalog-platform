# JS del catálogo público rediseñado al boceto del cliente: inicio, página de catálogo con filtros, pedido por WhatsApp y hojas.
CAT_JS = r'''// ---------- Catálogo público ----------
const NIGHT = '#15120D';
const ICON_SELLO = { calidad: 'leafshield', domicilio: 'moto', whatsapp: 'wa', precio: 'shield', tradicion: 'award', horario: 'clock' };
const br = s => esc(s).replace(/\n/g, '<br>'); // el dueño parte las líneas del titular con Enter
const efectivo = p => (S.empresa.precios && vigente(p) ? p.oferta : p.precio);
const esNuevo = p => !!p.creado && p.creado >= shift(-30);
const badge = p => !p.disp ? '<span class="tagb out">Agotado</span>'
  : S.empresa.precios && vigente(p) ? `<span class="tagb promo">Promo<span class="sr">, ${pct(p)}% menos</span></span>`
  : p.dest ? '<span class="tagb best">Más vendido</span>'
  : esNuevo(p) ? '<span class="tagb new">Nuevo</span>' : '';
const priceHtml = (p, cls = 'price') => {
  if (!S.empresa.precios) return `<span class="${cls} ask">Pregunta el precio</span>`;
  const u = p.unidad === 'UNIDAD' ? '' : ` <small>/ ${UNI[p.unidad]}</small>`;
  return vigente(p)
    ? `<span class="${cls}">${cop(p.oferta)}${u}<s><span class="sr">Antes </span>${cop(p.precio)}</s></span>`
    : `<span class="${cls}">${cop(p.precio)}${u}</span>`;
};
const rel = p => (p.disp ? 0 : 4) + (p.dest ? 0 : 1) + (S.empresa.precios && vigente(p) ? 0 : 1);
const sortRel = L => L.map((p, i) => [p, i]).sort((a, b) => rel(a[0]) - rel(b[0]) || a[1] - b[1]).map(x => x[0]);

// Pedido y favoritos: viven en el navegador del cliente (localStorage por negocio); el pedido solo sale al enviarlo por WhatsApp.
let cart = {}, favs = new Set();
const store = k => `ec-${k}-${S.empresa.hash}`;
function loadCart() {
  try { cart = JSON.parse(localStorage.getItem(store('pedido')) || '{}') || {}; } catch { cart = {}; }
  try { favs = new Set(JSON.parse(localStorage.getItem(store('favoritos')) || '[]')); } catch { favs = new Set(); }
  for (const id of Object.keys(cart)) if (!byId(+id)?.disp || !(cart[id] > 0)) delete cart[id];
}
function save(k, v) {
  try { localStorage.setItem(store(k), JSON.stringify(v)); } catch { /* sin almacenamiento dura mientras la página esté abierta */ }
}
const saveCart = () => save('pedido', cart);
const cartLines = () => Object.entries(cart).map(([id, n]) => ({ p: byId(+id), n })).filter(l => l.p);
const cartUnits = () => cartLines().reduce((s, l) => s + l.n, 0);
const cartTotal = () => cartLines().reduce((s, l) => s + efectivo(l.p) * l.n, 0);
function setQty(id, n) {
  if (n > 0) cart[id] = Math.min(n, 99); else delete cart[id];
  saveCart();
}
function orderMsg(f = {}) {
  const e = S.empresa;
  const lines = cartLines().map(({ p, n }) => `• ${n} × ${p.nombre} (${p.pres})${e.precios ? `: ${cop(efectivo(p) * n)}` : ''}`);
  return ['Hola, quiero hacer este pedido:', ...lines, e.precios ? `Total: ${cop(cartTotal())}` : '',
    f.nombre ? `Nombre: ${f.nombre}` : '', f.entrega ? `Entrega: ${f.entrega}` : ''].filter(Boolean).join('\n');
}
cart = { [S.prods.find(p => p.codigo === 'QS-001').id]: 2 };
console.assert(orderMsg({ nombre: 'Ana' }) === 'Hola, quiero hacer este pedido:\n• 2 × Queso campesino (Bloque): $24.000\nTotal: $24.000\nNombre: Ana', 'mensaje del pedido');
cart = {};

// Piezas del catálogo
const WAVE = '<svg class="wave" viewBox="0 0 1440 40" preserveAspectRatio="none" aria-hidden="true"><path d="M0 22C180 6 360 34 560 20S920 2 1120 16 1360 30 1440 18V40H0Z"/></svg>';
const NOTE_ARROW = '<svg class="note-arrow" viewBox="0 0 60 50" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M8 3c-3 17 6 32 33 38"/><path d="M29 33l12 8-12 4"/></svg>';
// Contáctanos lleva a la sección de contacto del inicio, antes del pie.
const NAV = () => {
  const combos = S.cats.find(c => c.activa && c.banner);
  return [['inicio', 'Inicio', ''], ['catalogo', 'Catálogo', '/catalogo'], ['promos', 'Promociones', '/catalogo?promo=1'],
    ...(combos ? [['combos', combos.nombre, `/catalogo?cat=${combos.id}`]] : []), ['contacto', 'Contáctanos', '/contacto']];
};
function activeNav(qs) {
  const q = new URLSearchParams(qs), only = k => [...q.keys()].length === 1 && q.has(k);
  const combos = S.cats.find(c => c.activa && c.banner);
  if (only('promo')) return 'promos';
  if (combos && only('cat') && q.get('cat') === String(combos.id)) return 'combos';
  return 'catalogo';
}
// «Salsamentaria» pequeño encima de «Javier» grande, como en el boceto; el lema va debajo.
function brandName(e) {
  const c = e.corto && e.nombre !== e.corto && e.nombre.endsWith(e.corto) ? e.corto : '';
  return `<span class="bn">${c ? `<small>${esc(e.nombre.slice(0, -c.length).trim())}</small>` : ''}<b>${esc(c || e.nombre)}</b>${e.lema ? `<i>${esc(e.lema)}</i>` : ''}</span>`;
}
function header(active) {
  const e = S.empresa, base = `#/c/${path(e)}`;
  return `<header class="top" id="top"><div class="top-in">
    <button class="icon-btn menu-btn" data-act="menu" aria-label="Menú">${ic('menu')}</button>
    <a class="brandlink" href="${base}" aria-label="${esc(e.nombre)}: inicio">${logo(e)}${brandName(e)}</a>
    <nav class="nav" aria-label="Secciones">${NAV().map(([k, t, h]) => `<a class="nav-a" href="${base}${h}"${k === active ? ' aria-current="page"' : ''}>${esc(t)}</a>`).join('')}</nav>
    <button class="icon-btn find-btn" data-act="find" aria-label="Buscar productos">${ic('search')}</button>
    ${e.pedidos ? `<button class="cart-btn" data-act="order"><span class="cart-ic">${ic('cart')}<span class="cart-n" data-cart-n hidden>0</span></span><span class="sr" data-cart-sr>Ver mi pedido</span><span class="cart-txt" aria-hidden="true"><small>Mi pedido</small><b data-cart-total></b></span></button>` : ''}
  </div></header>`;
}
const stamp = e => {
  const t = (e.selloTexto || e.nombre).toUpperCase(), ring = t.length > 20 ? `${t} · ` : `${t} · ${t} · `;
  const letter = (e.corto || e.nombre).trim().split(/\s+/).pop()[0].toUpperCase();
  return `<svg class="stamp" viewBox="0 0 140 140" aria-hidden="true"><defs><path id="stamp-ring" d="M70 70m-52 0a52 52 0 1 1 104 0a52 52 0 1 1-104 0"/></defs>
    <circle cx="70" cy="70" r="67" fill="rgba(21,18,13,.35)" stroke="#F1E9DC" stroke-width="1.6"/>
    <text fill="#F1E9DC" font-family="Onest,sans-serif" font-size="11" font-weight="600"><textPath href="#stamp-ring" textLength="322" lengthAdjust="spacing">${esc(ring)}</textPath></text>
    <text x="70" y="86" text-anchor="middle" fill="#fff" font-family="'Playfair Display',Georgia,serif" font-size="46" font-weight="700">${esc(letter)}</text></svg>`;
};
const searchHint = () => `Busca ${[...new Set(sortRel(visibles()).map(p => p.nombre.split(' ')[0].toLowerCase()))].slice(0, 4).join(', ')}…`;
function hero(e) {
  return `<section class="hero" aria-labelledby="hero-h">${e.portada ? `<img src="${e.portada}" alt="" fetchpriority="high">` : ''}
    ${e.antetitulo ? `<p class="eyebrow">${esc(e.antetitulo)}</p>` : ''}
    <h1 id="hero-h">${br(e.titulo || e.nombre)}${e.tituloAcento ? ` <em>${br(e.tituloAcento)}</em>` : ''}</h1>
    ${e.subtitulo ? `<p class="hero-sub">${esc(e.subtitulo)}</p>` : ''}
    <form class="hero-find" data-form="find" role="search"><label class="sr" for="hq">Buscar productos</label><input id="hq" name="q" type="search" placeholder="${esc(searchHint())}" enterkeyhint="search" autocomplete="off"><button aria-label="Buscar">${ic('search')}</button></form>
    <a class="hero-cta" href="#/c/${path(e)}/catalogo">Explorar el catálogo${ic('arrow')}</a>
    ${e.nota ? `<p class="hero-note" aria-hidden="true">${br(e.nota)}${NOTE_ARROW}</p>` : ''}${stamp(e)}
  </section>`;
}
const sellosHtml = e => e.sellos.length ? `<ul class="sellos">${e.sellos.map(s => `<li>${ic(ICON_SELLO[s.icono] || 'tag')}<span>${esc(s.texto)}</span></li>`).join('')}</ul>` : '';
const circles = e => `<nav class="circles" aria-label="Categorías">${S.cats.filter(c => c.activa).map(c => {
  const p = visibles().find(x => x.cat === c.id && src(x));
  return p ? `<a class="circle" href="#/c/${path(e)}/catalogo?cat=${c.id}">${thumb(p)}<span>${esc(c.nombre)}</span></a>` : '';
}).join('')}</nav>`;
function planes(e) {
  const vis = new Set(visibles().map(p => p.id)), L = S.cols.filter(c => c.visible && c.prods.some(id => vis.has(id)));
  if (!L.length) return '';
  return `<section class="planes" aria-labelledby="pl-h">${e.deco ? '<img class="deco deco-laurel" src="img/deco-laurel.webp" alt="" aria-hidden="true" loading="lazy" decoding="async"><img class="deco deco-perejil" src="img/deco-perejil.webp" alt="" aria-hidden="true" loading="lazy" decoding="async">' : ''}${e.colNota ? `<p class="side-note" aria-hidden="true">${br(e.colNota)}</p>` : ''}
    <h2 class="sec-title" id="pl-h">${esc(e.colTitulo)}</h2>${e.colTexto ? `<p class="sec-sub">${esc(e.colTexto)}</p>` : ''}
    <div class="plan-rail">${L.map(c => `<a class="plan" href="#/c/${path(e)}/catalogo?col=${c.id}">${c.img ? `<img src="${c.img}" alt="" loading="lazy">` : `<span class="ph-none">${ic('photo')}</span>`}
      <span class="plan-txt"><b>${esc(c.nombre)}</b>${c.desc ? `<span class="plan-desc">${esc(c.desc)}</span>` : ''}<span class="plan-go" aria-hidden="true">${ic('arrow')}</span></span></a>`).join('')}</div></section>`;
}
const FAV = { tab: 'todos' };
function favList(k) {
  const v = visibles();
  if (k === 'ofertas') return v.filter(p => S.empresa.precios && vigente(p) && p.disp);
  if (k === 'mas') return v.filter(p => p.dest);
  if (k[0] === 'c') return v.filter(p => p.cat === +k.slice(1));
  return sortRel(v);
}
const favTabs = () => {
  const v = visibles();
  return [['todos', 'Todos'], ...(S.empresa.precios && v.some(p => vigente(p) && p.disp) ? [['ofertas', 'Ofertas']] : []),
    ...(v.some(p => p.dest) ? [['mas', 'Más pedidos']] : []),
    ...S.cats.filter(c => c.activa && v.some(p => p.cat === c.id)).map(c => ['c' + c.id, c.nombre])];
};
const favHref = k => `#/c/${path(S.empresa)}/catalogo${k === 'ofertas' ? '?promo=1' : k === 'mas' ? '?mas=1' : k[0] === 'c' ? `?cat=${k.slice(1)}` : ''}`;
function favsSec() {
  if (!favTabs().some(([k]) => k === FAV.tab)) FAV.tab = 'todos';
  return `<section class="favs" aria-labelledby="fv-h"><div class="favs-head">
      <h2 class="script" id="fv-h">Nuestros favoritos</h2>
      <div class="chips" role="group" aria-label="Elegir qué productos ver">${favTabs().map(([k, t]) => `<button class="chip" data-act="fav" data-k="${k}" aria-pressed="${k === FAV.tab}">${esc(t)}</button>`).join('')}</div>
      <div class="favs-act"><a class="see-all" id="fv-all" href="${favHref(FAV.tab)}">Ver todos${ic('arrow')}</a>
        <button class="rail-btn" data-act="rail" data-d="-1" aria-label="Productos anteriores">${ic('back')}</button><button class="rail-btn" data-act="rail" data-d="1" aria-label="Más productos">${ic('chev')}</button></div>
    </div><div class="prow" id="fv-row">${favList(FAV.tab).slice(0, 12).map(pcard).join('')}</div>${S.empresa.deco ? '<img class="deco deco-laurel-b" src="img/deco-laurel.webp" alt="" aria-hidden="true" loading="lazy" decoding="async">' : ''}</section>`;
}
const qtyBox = (cls = '') => `<div class="qty ${cls}" role="group" aria-label="Cantidad"><button type="button" data-act="qty" data-d="-1" aria-label="Uno menos" aria-disabled="true">${ic('minus')}</button><output>1</output><button type="button" data-act="qty" data-d="1" aria-label="Uno más">${ic('plus')}</button></div>`;
const buyBox = p => !S.empresa.pedidos
  ? (hasWa() && p.disp ? `<div class="buy"><a class="btn ask-wa" ${waAttrs(msgProd(p))}>${ic('wa')}Preguntar</a></div>` : '')
  : p.disp
    ? `<div class="buy">${qtyBox()}<button type="button" class="add" data-act="add" data-id="${p.id}" aria-label="Agregar ${esc(p.nombre)} al pedido">${ic('cart')}</button></div>`
    : '<div class="buy"><span class="soon">Agotado por ahora</span></div>';
const heart = p => `<button type="button" class="heart" data-act="heart" data-id="${p.id}" aria-pressed="${favs.has(p.id)}" aria-label="Guardar ${esc(p.nombre)} en favoritos">${ic('heart')}</button>`;
const pcard = p => `<article class="pc${p.disp ? '' : ' out'}">${badge(p) ? `<span class="pc-badge">${badge(p)}</span>` : ''}${heart(p)}
  <button class="pc-hit" data-act="open" data-id="${p.id}">${thumb(p)}<span class="pc-body"><span class="pc-name">${esc(p.nombre)}</span>${p.desc ? `<span class="pc-desc">${esc(p.desc)}</span>` : '<span></span>'}
    <span class="pc-foot">${priceHtml(p)}${p.pres ? `<span class="pres">${esc(p.pres)}</span>` : ''}</span></span></button>${buyBox(p)}</article>`;
function band(e) {
  const c = S.cats.find(x => x.activa && x.banner && visibles().some(p => p.cat === x.id));
  if (!c) return '';
  return `<section class="band" aria-labelledby="bd-h">${c.banner.img ? `<img src="${c.banner.img}" alt="" loading="lazy">` : ''}${WAVE}
    ${c.banner.nota ? `<p class="band-tag" aria-hidden="true">${br(c.banner.nota)}</p>` : ''}
    <div class="band-in"><h2 id="bd-h">${br(c.banner.titulo || c.nombre)}</h2>${c.banner.frase ? `<p>${esc(c.banner.frase)}</p>` : ''}
      <a class="hero-cta" href="#/c/${path(e)}/catalogo?cat=${c.id}">Ver ${esc(c.nombre.toLowerCase())}${ic('arrow')}</a></div></section>`;
}
// Contacto: los números de WhatsApp son la acción principal; dirección, horario y domicilios van como datos de apoyo.
const mapsHref = e => `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${e.nombre}, ${e.direccion}`)}`;
const visitInfo = e => [['pin', 'Dirección', e.direccion, `<a class="v-link" href="${mapsHref(e)}" target="_blank" rel="noopener">Cómo llegar${ic('arrow')}</a>`],
  ['clock', 'Horario', e.horario], ['truck', 'Domicilios', e.domicilios]]
  .filter(r => r[2]).map(([, t, d, extra = '']) => `<div class="v-item"><dt>${t}</dt><dd><span>${esc(d).replace(/\. (?=[A-ZÁÉÍÓÚÑ])/g, '.<br>').replace(/(\d) ([ap])\. m\./g, '$1\u00a0$2.\u00a0m.').replace(/ a (?=\d)/g, ' a\u00a0')}</span>${extra}</dd></div>`).join('');
const waTiles = e => e.whatsapps.map(w => `<a class="v-wa" href="${waHref(MSG_GEN, w.numero)}" target="_blank" rel="noopener"><span class="v-wa-ic">${ic('wa')}</span>
  <span><small>${esc(w.nombre || 'WhatsApp')}</small><b>${phone(w.numero)}</b></span></a>`).join('');
// Fachadas dibujadas en línea (como el boceto del cliente). Comparten el local; cambian vitrinas y lo que va al frente.
const PLANTS = '<path d="M100 160l2-12h8l2 12M106 148c-6-6-6-14-1-18M106 148c5-6 7-12 3-17M106 148c-1-8 0-12 1-15"/><path d="M148 160l2-12h8l2 12M154 148c-6-6-6-14-1-18M154 148c5-6 7-12 3-17M154 148c-1-8 0-12 1-15"/>';
const SHOPS = {
  salsamentaria: { label: 'Salsamentaria',
    art: '<path d="M66 92v8M79 92v11M92 92v7"/><ellipse cx="66" cy="103" rx="2.6" ry="4"/><ellipse cx="79" cy="107" rx="2.6" ry="4.5"/><ellipse cx="92" cy="102" rx="2.6" ry="3.5"/><path d="M63 120a7 4 0 0 1 14 0M80 120l8-7 8 7"/>'
      + '<path d="M164 120a8 5 0 0 1 16 0M184 120l7-6 7 6M168 104h26"/><circle cx="174" cy="100" r="3"/><circle cx="186" cy="100" r="3"/>' + PLANTS },
  panaderia: { label: 'Panadería',
    art: '<path d="M62 120l3-9h28l3 9"/><path d="M68 111l-2-14a2.5 2.5 0 0 1 5 0l1 14M76 111l1-16a2.5 2.5 0 0 1 5 .5l-1 15.5M85 111l3-13a2.5 2.5 0 0 1 5 1l-3 12"/>'
      + '<path d="M162 120a9 7 0 0 1 18 0zM182 120a10 8 0 0 1 20 0z"/><path d="M167 116l2.5-3M172 115l2.5-3M188 116l2.5-3M194 115l2.5-3"/><path d="M170 106c2-6 14-6 16 0c-3-2-6-2-8 1c-2-3-5-3-8-1z"/>' + PLANTS },
  fruteria: { label: 'Frutería',
    art: [[63, 116], [71, 116], [79, 116], [87, 116], [95, 116], [67, 109], [75, 109], [83, 109], [91, 109], [71, 102], [79, 102], [87, 102]].map(([x, y]) => `<circle cx="${x}" cy="${y}" r="3.6"/>`).join('')
      + '<path d="M164 110c5 7 20 7 26-3c-6 4-18 4-26 3zM166 104c5 7 20 7 26-3"/><path d="M195 120c-5 0-6-6-3-9c1-2 0-5 3-5s2 3 3 5c3 3 2 9-3 9zM195 106v-3"/>'
      + '<rect x="86" y="148" width="22" height="12" rx="1"/><path d="M86 154h22"/><circle cx="91" cy="145" r="3"/><circle cx="97" cy="144" r="3"/><circle cx="103" cy="145" r="3"/>'
      + '<rect x="152" y="148" width="22" height="12" rx="1"/><path d="M152 154h22"/><circle cx="157" cy="145" r="3"/><circle cx="163" cy="144" r="3"/><circle cx="169" cy="145" r="3"/>' },
  cafeteria: { label: 'Cafetería',
    art: '<path d="M66 104h20v6a8 8 0 0 1-8 8h-4a8 8 0 0 1-8-8z"/><path d="M86 106h3a3 3 0 0 1 0 6h-3"/><path d="M72 100c-2-2.5 2-3.5 0-6M79 100c-2-2.5 2-3.5 0-6"/>'
      + '<path d="M164 118h20v-11z"/><path d="M168 114h16"/><path d="M190 118l-2-7h12l-2 7zM188 111c0-5 12-5 12 0"/><circle cx="194" cy="104" r="1.5"/>' + PLANTS },
  tienda: { label: 'Minimercado',
    art: '<path d="M56 106h46"/><rect x="61" y="97" width="7" height="9" rx="1"/><rect x="72" y="97" width="7" height="9" rx="1"/><rect x="83" y="97" width="7" height="9" rx="1"/>'
      + [62, 72, 82, 92].map(x => `<path d="M${x} 120v-8l2-2v-3h2v3l2 2v8z"/>`).join('')
      + '<rect x="162" y="108" width="10" height="12"/><rect x="174" y="104" width="12" height="16"/><rect x="188" y="110" width="12" height="10"/><path d="M174 110h12M162 113h10"/>' + PLANTS },
  ferreteria: { label: 'Ferretería',
    art: '<path d="M66 118l14-14"/><path d="M76 100l6-6 8 8-6 6z"/><path d="M86 118l7-7"/><circle cx="96" cy="108" r="3.5"/>'
      + '<rect x="164" y="106" width="16" height="14" rx="1.5"/><path d="M164 110h16M168 106c0-4 8-4 8 0"/><path d="M188 120v-10M194 120v-10M200 120v-10M186 110h4M192 110h4M198 110h4"/>' + PLANTS },
};
const shopArt = (e, kind) => `<svg class="v-shop" data-kind="${kind}" viewBox="0 0 260 168" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
  <path d="M14 160h232"/><path d="M44 74v86M216 74v86"/><path d="M38 52h184M44 52V40h172v12"/>
  <rect x="70" y="24" width="120" height="22" rx="3"/>
  <text x="130" y="39.5" text-anchor="middle" fill="currentColor" stroke="none" font-family="'Playfair Display',Georgia,serif" font-size="10.5" font-weight="700" letter-spacing="1.6" textLength="104" lengthAdjust="spacingAndGlyphs">${esc((e.nombre || '').toUpperCase())}</text>
  <path d="M36 56h188v10"/><path d="M36 56v10"/>
  <path d="M36 66a7.8 7.8 0 0 0 15.6 0a7.8 7.8 0 0 0 15.6 0a7.8 7.8 0 0 0 15.6 0a7.8 7.8 0 0 0 15.6 0a7.8 7.8 0 0 0 15.6 0a7.8 7.8 0 0 0 15.6 0a7.8 7.8 0 0 0 15.6 0a7.8 7.8 0 0 0 15.6 0a7.8 7.8 0 0 0 15.6 0a7.8 7.8 0 0 0 15.6 0a7.8 7.8 0 0 0 15.6 0a7.8 7.8 0 0 0 15.6 0"/>
  <path d="M67.2 56v10M98.4 56v10M129.6 56v10M160.8 56v10M192 56v10"/>
  <rect x="56" y="92" width="46" height="40" rx="2"/><path d="M56 120h46"/><rect x="158" y="92" width="46" height="40" rx="2"/><path d="M158 120h46"/>
  <rect x="114" y="94" width="32" height="66" rx="2"/><rect x="120" y="100" width="20" height="22" rx="1.5"/><circle cx="140" cy="132" r="1.6" fill="currentColor"/>
  <path d="M130 84v6M126 90h8l-1.5 4h-5z"/>${SHOPS[kind].art}</svg>`;
const visit = e => {
  const n = e.whatsapps.length;
  return `<section class="visit" id="contacto" aria-labelledby="vs-h">${WAVE}<div class="v-in">
    ${SHOPS[e.fachada] ? shopArt(e, e.fachada) : ''}${e.visitaNota ? `<p class="visit-note" aria-hidden="true">${br(e.visitaNota)}</p>` : ''}
    <h2 id="vs-h">Nos vemos en <em>${esc(e.corto || e.nombre)}.</em></h2>
    <p class="v-sub">${n ? 'Escríbenos por WhatsApp para pedir o preguntar, o pasa por el local.' : 'Pasa por el local; te atendemos con gusto.'}</p>
    ${n ? `<div class="v-was">${waTiles(e)}</div>` : ''}
    <dl class="v-info">${visitInfo(e)}</dl>
  </div></section>`;
};
const foot = e => {
  const base = `#/c/${path(e)}`;
  return `<footer class="foot"><div class="foot-in">
    <div class="foot-brand"><a class="brandlink" href="${base}" aria-label="${esc(e.nombre)}: inicio">${logo(e)}${brandName(e)}</a>${e.subtitulo ? `<p>${esc(e.subtitulo)}</p>` : ''}</div>
    <nav class="foot-nav" aria-labelledby="ft-nav"><h3 id="ft-nav">Explora</h3>${NAV().map(([, t, h]) => `<a href="${base}${h}">${esc(t)}</a>`).join('')}</nav>
    <div class="foot-share"><h3>Comparte el catálogo</h3><p>Envíalo a tu familia o a tus amigos por WhatsApp o redes.</p><button class="btn foot-btn" data-act="share">${ic('share')}Compartir catálogo</button></div>
  </div>
  <div class="foot-legal"><span>© ${new Date().getFullYear()} ${esc(e.nombre)}.${e.precios ? '' : ' Pregunta precios y disponibilidad por WhatsApp.'}</span>
    <span class="foot-made">Catálogo hecho con <img class="ec" src="marca/logo-dark.svg" alt="Agorá Catalog" width="132" height="28"></span></div>
  <p class="proto-note">Prototipo con datos de ejemplo. Las fotos son de prueba, tomadas de Wikimedia Commons; no son del negocio. <button data-act="credits">Ver créditos</button></p></footer>`;
};
function paintCart(bump = false) {
  const e = S.empresa, n = cartUnits();
  $$('[data-cart-n]').forEach(el => { el.textContent = n; el.hidden = !n; });
  $$('[data-cart-total]').forEach(el => { el.textContent = e.precios ? cop(cartTotal()) : plural(n, 'producto'); });
  $$('[data-cart-sr]').forEach(el => { el.textContent = n ? `Ver mi pedido: ${plural(n, 'producto')}${e.precios ? `, ${cop(cartTotal())}` : ''}` : 'Ver mi pedido'; });
  if (bump) $$('.cart-ic').forEach(el => { el.classList.remove('bump'); void el.offsetWidth; el.classList.add('bump'); });
  if ($('#order')) {
    const d = document.activeElement?.dataset || {};
    const keep = d.act === 'line' ? `[data-act="line"][data-id="${d.id}"][data-d="${d.d}"]` : null;
    $('#order').innerHTML = orderBody();
    $('#o-sum').innerHTML = orderSum();
    $('#o-form').hidden = !n;
    ((keep && $(keep)) || $('.sheet-close')).focus();
  }
}

let focusSearch = false;
function catalogo(arg, sub = [], qs = '') {
  const e = S.empresa;
  if ((arg || '').split('-').pop() !== e.hash || !e.activa) return noDisponible();
  if (arg !== path(e)) return location.replace(`#/c/${path(e)}${sub.length ? '/' + sub.join('/') : ''}${qs ? '?' + qs : ''}`); // en producción: 301 del backend
  $('meta[name="theme-color"]').content = NIGHT;
  document.documentElement.style.setProperty('--brand', e.primario);
  document.documentElement.style.setProperty('--accent', e.secundario);
  loadCart();
  const page = sub[0] === 'catalogo' ? 'catalogo' : 'inicio';
  document.title = page === 'inicio' ? `${e.nombre}: catálogo` : `Catálogo de ${e.nombre}`;
  const body = page === 'inicio'
    ? `${hero(e)}${sellosHtml(e)}<div class="counter">${WAVE}${circles(e)}${planes(e)}${favsSec()}</div>${band(e)}${visit(e)}`
    : catPage(e, new URLSearchParams(qs)) + band(e);
  const active = page === 'catalogo' ? activeNav(qs) : sub[0] === 'contacto' ? 'contacto' : 'inicio';
  app.innerHTML = `<div class="store${page === 'inicio' ? ' home' : ''}">${header(active)}<main>${body}</main>${foot(e)}</div>`;
  paintCart();
  paintTop();
  if (page === 'catalogo') {
    paintResults();
    if (focusSearch) { focusSearch = false; $('#cq').focus({ preventScroll: true }); }
  }
  if (active === 'contacto') requestAnimationFrame(() => $('#contacto').scrollIntoView({ behavior: motion() })); // después del scroll arriba de render()
}
const paintTop = () => $('#top')?.classList.toggle('solid', scrollY > 24 || !$('.hero'));
addEventListener('scroll', paintTop, { passive: true });

// Página de catálogo: filtros en la URL para poder compartir el link de una búsqueda.
const FIL = {};
function readFil(qp) {
  Object.assign(FIL, {
    cats: new Set((qp.get('cat') || '').split(',').filter(Boolean).map(Number)), promo: qp.get('promo') === '1', mas: qp.get('mas') === '1',
    disp: qp.get('disp') === '1', favs: qp.get('fav') === '1', col: +qp.get('col') || 0, q: qp.get('q') || '', min: +qp.get('min') || 0, max: +qp.get('max') || 0,
    sort: qp.get('orden') || 'rel', lista: qp.get('vista') === 'lista',
  });
}
function filQs() {
  const q = new URLSearchParams();
  if (FIL.cats.size) q.set('cat', [...FIL.cats].join(','));
  for (const k of ['promo', 'mas', 'disp']) if (FIL[k]) q.set(k, '1');
  if (FIL.favs) q.set('fav', '1');
  if (FIL.col) q.set('col', FIL.col);
  if (FIL.q) q.set('q', FIL.q);
  if (FIL.min) q.set('min', FIL.min);
  if (FIL.max) q.set('max', FIL.max);
  if (FIL.sort !== 'rel') q.set('orden', FIL.sort);
  if (FIL.lista) q.set('vista', 'lista');
  return q.toString();
}
readFil(new URLSearchParams('cat=1,3&promo=1&q=queso&orden=menor'));
console.assert(filQs() === 'cat=1%2C3&promo=1&q=queso&orden=menor' && FIL.cats.has(3) && FIL.sort === 'menor', 'filtros en la URL');
function bounds() {
  const ps = visibles().map(efectivo);
  return ps.length ? [Math.floor(Math.min(...ps) / 1000) * 1000, Math.ceil(Math.max(...ps) / 1000) * 1000] : [0, 0];
}
function filtered() {
  const q = norm(FIL.q), col = S.cols.find(c => c.id === FIL.col), pr = S.empresa.precios;
  const L = visibles().filter(p => (!FIL.cats.size || FIL.cats.has(p.cat)) && (!FIL.promo || (pr && vigente(p))) && (!FIL.mas || p.dest)
    && (!FIL.disp || p.disp) && (!FIL.favs || favs.has(p.id)) && (!col || col.prods.includes(p.id))
    && (!q || norm(`${p.nombre} ${p.pres} ${p.desc} ${catOf(p).nombre}`).includes(q))
    && (!pr || ((!FIL.min || efectivo(p) >= FIL.min) && (!FIL.max || efectivo(p) <= FIL.max))));
  if (FIL.sort === 'menor') return L.sort((a, b) => efectivo(a) - efectivo(b));
  if (FIL.sort === 'mayor') return L.sort((a, b) => efectivo(b) - efectivo(a));
  if (FIL.sort === 'az') return L.sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));
  return sortRel(L);
}
function filtersHtml(inSheet = false) {
  const v = visibles(), e = S.empresa, [lo, hi] = bounds();
  const nPromo = e.precios ? v.filter(vigente).length : 0, nFav = v.filter(p => favs.has(p.id)).length;
  const check = (attr, on, label, n) => `<label class="f-check"><input type="checkbox" ${attr}${on ? ' checked' : ''}>${label} <small>(${n})</small></label>`;
  const groups = `<div class="f-group"><h3>Categoría</h3>${S.cats.filter(c => c.activa && v.some(p => p.cat === c.id)).map(c => check(`data-fcat="${c.id}"`, FIL.cats.has(c.id), esc(c.nombre), v.filter(p => p.cat === c.id).length)).join('')}</div>
    ${e.precios && hi > lo ? `<div class="f-group"><h3>Precio</h3><p class="range-txt">${cop(FIL.min || lo)} - ${cop(FIL.max || hi)}</p><div class="range"><input type="range" data-fmin min="${lo}" max="${hi}" step="500" value="${FIL.min || lo}" aria-label="Precio mínimo"><input type="range" data-fmax min="${lo}" max="${hi}" step="500" value="${FIL.max || hi}" aria-label="Precio máximo"></div></div>` : ''}
    ${nPromo ? `<div class="f-group"><h3>Promociones</h3>${check('data-fflag="promo"', FIL.promo, 'Solo promociones', nPromo)}</div>` : ''}
    <div class="f-group"><h3>Disponibilidad</h3>${check('data-fflag="disp"', FIL.disp, 'En stock', v.filter(p => p.disp).length)}</div>
    ${nFav ? `<div class="f-group"><h3>Favoritos</h3>${check('data-fflag="favs"', FIL.favs, 'Mis favoritos', nFav)}</div>` : ''}`;
  if (inSheet) return groups;
  return `<div class="f-card">${groups}</div>${hasWa() ? `<div class="help-card"><h3>¿No encuentras algo en específico?</h3><p>Escríbenos por WhatsApp y te ayudamos.</p><a class="btn" ${waAttrs('Hola, busco un producto que no encontré en el catálogo.')}>${ic('wa')}Hablar por WhatsApp</a></div>` : ''}`;
}
function catPage(e, qp) {
  readFil(qp);
  const h1 = e.catTitulo ? `${br(e.catTitulo)}${e.catAcento ? ` <em>${br(e.catAcento)}</em>` : ''}` : 'Nuestro catálogo';
  return `<section class="cat-hero">${e.portada ? `<img src="${e.portada}" alt="">` : ''}<div class="cat-hero-in">${e.catTitulo ? '<p class="eyebrow">Nuestro catálogo</p>' : ''}<h1>${h1}</h1>
      <p>${esc(e.catSubtitulo || `${plural(visibles().length, 'producto')} de ${e.nombre}.`)}</p></div></section>
    <div class="cat-wrap"><nav class="crumbs" aria-label="Ruta"><a href="#/c/${path(e)}">${ic('back')}Inicio</a><span aria-hidden="true">/</span><span aria-current="page">Catálogo</span></nav>
      <div class="cat-tools">
        <form class="c-search" data-form="csearch" role="search">${ic('search')}<label class="sr" for="cq">Buscar productos</label><input id="cq" type="search" value="${esc(FIL.q)}" placeholder="${esc(searchHint())}" autocomplete="off" enterkeyhint="search"><button aria-label="Buscar">${ic('search')}</button></form>
        <button type="button" class="btn filters-btn" data-act="filters">${ic('filter')}Filtros<span id="fcount"></span></button>
        <select class="c-select" id="corden" aria-label="Ordenar">${[['rel', 'Más relevantes'], ['menor', 'Menor precio'], ['mayor', 'Mayor precio'], ['az', 'Nombre de la A a la Z']].map(([v, t]) => `<option value="${v}"${FIL.sort === v ? ' selected' : ''}>${t}</option>`).join('')}</select>
        <div class="view-tog" role="group" aria-label="Vista"><button type="button" data-act="vista" data-v="grid" aria-pressed="${!FIL.lista}" aria-label="Cuadrícula">${ic('grid')}</button><button type="button" data-act="vista" data-v="lista" aria-pressed="${FIL.lista}" aria-label="Lista">${ic('list')}</button></div>
      </div>
      <div class="cat-layout"><aside class="filters" id="filters" aria-label="Filtros">${filtersHtml()}</aside>
        <div><div class="res-head"><p id="rcount" aria-live="polite"></p><div class="active-f" id="active-f"></div></div><div class="grid" id="results"></div></div></div>
    </div>`;
}
function activeChips() {
  const L = [], col = S.cols.find(c => c.id === FIL.col), [lo, hi] = bounds();
  if (col) L.push(['col', col.nombre]);
  FIL.cats.forEach(id => { const c = S.cats.find(x => x.id === id); if (c) L.push([`cat:${id}`, c.nombre]); });
  if (FIL.promo) L.push(['promo', 'Promociones']);
  if (FIL.mas) L.push(['mas', 'Más pedidos']);
  if (FIL.disp) L.push(['disp', 'En stock']);
  if (FIL.favs) L.push(['favs', 'Mis favoritos']);
  if (FIL.min || FIL.max) L.push(['precio', `${cop(FIL.min || lo)} - ${cop(FIL.max || hi)}`]);
  return L.map(([k, t]) => `<button class="fchip" data-act="unfil" data-k="${k}" aria-label="Quitar el filtro ${esc(t)}">${esc(t)}${ic('x')}</button>`).join('');
}
const nActive = () => (FIL.col ? 1 : 0) + FIL.cats.size + FIL.promo + FIL.mas + FIL.disp + FIL.favs + (FIL.min || FIL.max ? 1 : 0);
// Tarjeta «Pide en tres pasos»: llena las celdas vacías de la última fila; si la fila está completa, no sale.
const howTile = () => S.empresa.pedidos && hasWa() ? `<aside class="how" aria-label="Cómo pedir"><h3>Pide en tres pasos</h3><ol>
  <li>Elige tus productos y cuántos quieres.</li><li>Revisa la lista en «Mi pedido».</li><li>Envíala por WhatsApp: ${esc(S.empresa.corto || S.empresa.nombre)} te confirma total y domicilio.</li></ol><button type="button" class="btn how-btn" data-act="order">${ic('cart')}Ver mi pedido</button></aside>` : '';
function fitHow() {
  const box = $('#results'), t = box && box.querySelector('.how');
  if (!t) return;
  const cols = getComputedStyle(box).gridTemplateColumns.split(' ').length, rest = (cols - box.querySelectorAll('.pc').length % cols) % cols;
  t.hidden = FIL.lista || !rest;
  t.style.gridColumn = `span ${rest || 1}`;
  t.classList.toggle('wide', rest > 1);
}
addEventListener('resize', fitHow);
function paintResults() {
  const L = filtered(), box = $('#results');
  if (!box) return;
  box.className = 'grid' + (FIL.lista ? ' as-list' : '');
  box.innerHTML = L.length ? L.map(pcard).join('') + howTile()
    : `<div class="no-hits"><b>${FIL.q ? `No encontramos “${esc(FIL.q)}”` : 'Ningún producto con estos filtros'}</b><span>Prueba con otra palabra o quita filtros${hasWa() ? ', o pregúntanos por WhatsApp' : ''}.</span><button class="btn" data-act="clear-fil">Quitar filtros</button></div>`;
  fitHow();
  $('#rcount').textContent = plural(L.length, 'resultado');
  $('#active-f').innerHTML = activeChips();
  $('#fcount').textContent = nActive() ? ` (${nActive()})` : '';
  if ($('#f-done')) $('#f-done').textContent = `Ver ${plural(L.length, 'resultado')}`;
  const qs = filQs();
  history.replaceState(null, '', `#/c/${path(S.empresa)}/catalogo${qs ? '?' + qs : ''}`);
}
function syncFilters(from) {
  if (from?.closest('.fsheet')) $('#filters').innerHTML = filtersHtml();
}
function refreshFilters() {
  if ($('#filters')) $('#filters').innerHTML = filtersHtml();
  if ($('.f-body')) $('.f-body').innerHTML = filtersHtml(true);
}
function openFilters() {
  sheet(`<div class="sheet-scroll fsheet"><h2>Filtros</h2><div class="f-body">${filtersHtml(true)}</div></div>
    <div class="sheet-cta"><button class="btn btn-brand" data-act="close" id="f-done">Ver resultados</button></div>`, 'Filtros', 'cat-sheet');
  $('#f-done').textContent = `Ver ${plural(filtered().length, 'resultado')}`;
}

// Hojas
function openProd(id) {
  const p = byId(id), e = S.empresa;
  const buy = e.pedidos && p.disp ? `<div class="ps-buy">${qtyBox('big')}<button class="btn btn-brand" data-act="add" data-id="${p.id}" data-close="1">${ic('cart')}Agregar al pedido</button></div>` : '';
  const ask = hasWa() ? `<a class="${buy ? 'ps-ask' : 'btn btn-brand ps-ask-btn'}" ${waAttrs(msgProd(p))}>${ic('wa')}${p.disp ? 'Preguntar por WhatsApp' : 'Preguntar cuándo llega'}</a>` : '';
  sheet(`<span class="grab" aria-hidden="true"></span><div class="sheet-scroll">${badge(p) ? `<span class="pc-badge">${badge(p)}</span>` : ''}${thumb(p, 'sheet-ph', true)}
    <div class="ps-body"><h2>${esc(p.nombre)}</h2><p class="muted">${esc(p.pres)}</p>${p.disp ? priceHtml(p, 'price big') : ''}${p.desc ? `<p class="ps-desc">${esc(p.desc)}</p>` : ''}</div></div>
    ${buy || ask ? `<div class="ps-foot">${buy}${ask}</div>` : ''}`, p.nombre, 'cat-sheet');
}
function orderBody() {
  const L = cartLines(), e = S.empresa;
  if (!L.length) return `<h2>Mi pedido</h2><div class="o-empty"><p>Tu pedido está vacío. Suma productos con el botón del carrito.</p><a class="btn" href="#/c/${path(e)}/catalogo">Explorar el catálogo</a></div>`;
  return `<h2>Mi pedido</h2><ul class="olines">${L.map(({ p, n }) => `<li class="oline">${thumb(p)}
    <div class="oline-txt"><b>${esc(p.nombre)}</b><span class="muted">${esc(p.pres)}</span>${e.precios ? `<span class="oline-sub">${cop(efectivo(p) * n)}${n > 1 ? `<small> (${cop(efectivo(p))} cada uno)</small>` : ''}</span>` : ''}</div>
    <div class="qty sm" role="group" aria-label="Cantidad de ${esc(p.nombre)}"><button type="button" data-act="line" data-id="${p.id}" data-d="-1" aria-label="${n === 1 ? `Quitar ${esc(p.nombre)}` : 'Uno menos'}">${ic(n === 1 ? 'trash' : 'minus')}</button><output>${n}</output><button type="button" data-act="line" data-id="${p.id}" data-d="1" aria-label="Uno más"${n >= 99 ? ' disabled' : ''}>${ic('plus')}</button></div></li>`).join('')}</ul>`;
}
function orderSum() {
  const e = S.empresa;
  if (!cartUnits()) return '';
  return e.precios
    ? `<div class="total"><span>Total</span><b>${cop(cartTotal())}</b></div><p class="hint">${esc(e.nombre)} te confirma el total y el domicilio por WhatsApp.</p>`
    : `<p class="hint">${esc(e.nombre)} te confirma precios y domicilio por WhatsApp.</p>`;
}
function openOrder() {
  sheet(`<div class="sheet-scroll"><div class="order" id="order">${orderBody()}</div></div>
    <div class="order-foot"><div id="o-sum">${orderSum()}</div>
      <form class="o-form" id="o-form" data-form="order" novalidate${cartUnits() ? '' : ' hidden'}>
        <div class="o-fields"><label>Tu nombre (opcional)<input class="input" name="nombre" autocomplete="name"></label>
          <label>Dirección, o «paso a recoger» (opcional)<input class="input" name="entrega" autocomplete="street-address"></label></div>
        ${hasWa() ? `<button class="btn btn-brand">${ic('wa')}Enviar pedido por WhatsApp</button>` : `<p class="hint">${esc(S.empresa.nombre)} todavía no tiene WhatsApp en su catálogo.</p>`}
        <button type="button" class="btn link" data-act="empty">Vaciar pedido</button></form></div>`, 'Mi pedido', 'cat-sheet drawer');
}
function submitOrder(f) {
  if (!cartUnits() || !hasWa()) return;
  const v = Object.fromEntries(new FormData(f));
  const msg = orderMsg({ nombre: v.nombre.trim(), entrega: v.entrega.trim() });
  if (S.empresa.whatsapps.length > 1) return pickWa({ dataset: { msg } });
  window.open(waHref(msg), '_blank', 'noopener');
  toast('Abrimos WhatsApp con tu pedido');
}
function openMenu() {
  const e = S.empresa, base = `#/c/${path(e)}`;
  sheet(`<div class="sheet-scroll"><nav class="menu" aria-label="Secciones">${NAV().map(([, t, h]) => `<a href="${base}${h}" data-act="close">${esc(t)}</a>`).join('')}</nav>
    <div class="menu-foot">${hasWa() ? `<a class="btn btn-brand" ${waAttrs()}>${ic('wa')}Escribir por WhatsApp</a>` : ''}<button class="btn" data-act="share">${ic('share')}Compartir catálogo</button>${ecLogo()}</div></div>`, 'Menú', 'cat-sheet');
}
function pickWa(t) {
  const msg = t.dataset.msg || MSG_GEN;
  sheet(`<div class="sheet-scroll m-pad"><h2>¿A qué número le escribes?</h2>
    <p class="muted">${t.dataset.msg ? 'El mensaje ya va escrito.' : `${esc(S.empresa.nombre)} te atiende en cualquiera de estos números.`}</p>
    <div class="wa-list">${S.empresa.whatsapps.map(w => `<a class="btn btn-brand" href="${waHref(msg, w.numero)}" target="_blank" rel="noopener">${ic('wa')}<span>${w.nombre ? `${esc(w.nombre)}<small>${phone(w.numero)}</small>` : phone(w.numero)}</span></a>`).join('')}</div></div>`, 'Elegir número de WhatsApp', 'cat-sheet');
}
async function openCredits() {
  let list;
  try { list = await (await fetch('img/creditos.json')).json(); }
  catch { return toast('No se pudieron cargar los créditos.'); }
  sheet(`<div class="sheet-scroll sheet-info"><h2>Créditos de las fotos</h2>
    <p class="muted">Fotos de prueba de Wikimedia Commons, usadas solo en este prototipo. No son de ${esc(S.empresa.nombre)}.</p>
    <ul class="cred-list">${list.map(c => `<li><b>${esc(c.titulo.replace(/^File:/, ''))}</b><br>${esc(c.autor)}. Licencia ${esc(c.licencia)}. <a href="${esc(c.fuente)}" target="_blank" rel="noopener">Ver en Commons</a></li>`).join('')}</ul></div>`, 'Créditos de las fotos', 'cat-sheet');
}
async function share() {
  try {
    if (navigator.share) await navigator.share({ title: S.empresa.nombre, url: publicUrl() });
    else await copy(publicUrl());
  } catch { /* el usuario cerró el diálogo de compartir */ }
}
function noDisponible() {
  document.title = 'Catálogo no disponible';
  document.body.dataset.surface = 'panel'; // sin catálogo activo solo queda la marca Agorá
  app.innerHTML = `<main class="na"><div class="na-box"><span class="na-icon">${ic('store')}</span><h1>Catálogo no disponible</h1>
    <p>Este link no lleva a un catálogo activo. Pídele al negocio que te lo comparta de nuevo.</p>${ecLogo()}</div></main>`;
}

'''
