import {Fachada} from './catalogo.models';

/**
 * Fachadas dibujadas en línea para la sección de contacto (como el boceto del cliente). Comparten el
 * local; cambian vitrinas y lo que va al frente. Trazos tomados tal cual del prototipo.
 */
const PLANTAS = '<path d="M100 160l2-12h8l2 12M106 148c-6-6-6-14-1-18M106 148c5-6 7-12 3-17M106 148c-1-8 0-12 1-15"/><path d="M148 160l2-12h8l2 12M154 148c-6-6-6-14-1-18M154 148c5-6 7-12 3-17M154 148c-1-8 0-12 1-15"/>';

const ARTE: Record<Fachada, string> = {
  SALSAMENTARIA: '<path d="M66 92v8M79 92v11M92 92v7"/><ellipse cx="66" cy="103" rx="2.6" ry="4"/><ellipse cx="79" cy="107" rx="2.6" ry="4.5"/><ellipse cx="92" cy="102" rx="2.6" ry="3.5"/><path d="M63 120a7 4 0 0 1 14 0M80 120l8-7 8 7"/>'
    + '<path d="M164 120a8 5 0 0 1 16 0M184 120l7-6 7 6M168 104h26"/><circle cx="174" cy="100" r="3"/><circle cx="186" cy="100" r="3"/>' + PLANTAS,
  PANADERIA: '<path d="M62 120l3-9h28l3 9"/><path d="M68 111l-2-14a2.5 2.5 0 0 1 5 0l1 14M76 111l1-16a2.5 2.5 0 0 1 5 .5l-1 15.5M85 111l3-13a2.5 2.5 0 0 1 5 1l-3 12"/>'
    + '<path d="M162 120a9 7 0 0 1 18 0zM182 120a10 8 0 0 1 20 0z"/><path d="M167 116l2.5-3M172 115l2.5-3M188 116l2.5-3M194 115l2.5-3"/><path d="M170 106c2-6 14-6 16 0c-3-2-6-2-8 1c-2-3-5-3-8-1z"/>' + PLANTAS,
  FRUTERIA: [[63, 116], [71, 116], [79, 116], [87, 116], [95, 116], [67, 109], [75, 109], [83, 109], [91, 109], [71, 102], [79, 102], [87, 102]]
      .map(([x, y]) => `<circle cx="${x}" cy="${y}" r="3.6"/>`).join('')
    + '<path d="M164 110c5 7 20 7 26-3c-6 4-18 4-26 3zM166 104c5 7 20 7 26-3"/><path d="M195 120c-5 0-6-6-3-9c1-2 0-5 3-5s2 3 3 5c3 3 2 9-3 9zM195 106v-3"/>'
    + '<rect x="86" y="148" width="22" height="12" rx="1"/><path d="M86 154h22"/><circle cx="91" cy="145" r="3"/><circle cx="97" cy="144" r="3"/><circle cx="103" cy="145" r="3"/>'
    + '<rect x="152" y="148" width="22" height="12" rx="1"/><path d="M152 154h22"/><circle cx="157" cy="145" r="3"/><circle cx="163" cy="144" r="3"/><circle cx="169" cy="145" r="3"/>',
  CAFETERIA: '<path d="M66 104h20v6a8 8 0 0 1-8 8h-4a8 8 0 0 1-8-8z"/><path d="M86 106h3a3 3 0 0 1 0 6h-3"/><path d="M72 100c-2-2.5 2-3.5 0-6M79 100c-2-2.5 2-3.5 0-6"/>'
    + '<path d="M164 118h20v-11z"/><path d="M168 114h16"/><path d="M190 118l-2-7h12l-2 7zM188 111c0-5 12-5 12 0"/><circle cx="194" cy="104" r="1.5"/>' + PLANTAS,
  MINIMERCADO: '<path d="M56 106h46"/><rect x="61" y="97" width="7" height="9" rx="1"/><rect x="72" y="97" width="7" height="9" rx="1"/><rect x="83" y="97" width="7" height="9" rx="1"/>'
    + [62, 72, 82, 92].map(x => `<path d="M${x} 120v-8l2-2v-3h2v3l2 2v8z"/>`).join('')
    + '<rect x="162" y="108" width="10" height="12"/><rect x="174" y="104" width="12" height="16"/><rect x="188" y="110" width="12" height="10"/><path d="M174 110h12M162 113h10"/>' + PLANTAS,
  FERRETERIA: '<path d="M66 118l14-14"/><path d="M76 100l6-6 8 8-6 6z"/><path d="M86 118l7-7"/><circle cx="96" cy="108" r="3.5"/>'
    + '<rect x="164" y="106" width="16" height="14" rx="1.5"/><path d="M164 110h16M168 106c0-4 8-4 8 0"/><path d="M188 120v-10M194 120v-10M200 120v-10M186 110h4M192 110h4M198 110h4"/>' + PLANTAS,
};

const esc = (s: string) => s.replace(/[&<>"']/g, c => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', '\'': '&#39;'}[c]!));

/** SVG completo del local con el nombre del negocio en el letrero (escapado: viene del dueño). */
export function fachadaSvg(tipo: Fachada, nombre: string): string {
  return `<svg class="v-shop" viewBox="0 0 260 168" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
  <path d="M14 160h232"/><path d="M44 74v86M216 74v86"/><path d="M38 52h184M44 52V40h172v12"/>
  <rect x="70" y="24" width="120" height="22" rx="3"/>
  <text x="130" y="39.5" text-anchor="middle" fill="currentColor" stroke="none" font-family="'Playfair Display',Georgia,serif" font-size="10.5" font-weight="700" letter-spacing="1.6" textLength="104" lengthAdjust="spacingAndGlyphs">${esc(nombre.toUpperCase())}</text>
  <path d="M36 56h188v10"/><path d="M36 56v10"/>
  <path d="M36 66a7.8 7.8 0 0 0 15.6 0a7.8 7.8 0 0 0 15.6 0a7.8 7.8 0 0 0 15.6 0a7.8 7.8 0 0 0 15.6 0a7.8 7.8 0 0 0 15.6 0a7.8 7.8 0 0 0 15.6 0a7.8 7.8 0 0 0 15.6 0a7.8 7.8 0 0 0 15.6 0a7.8 7.8 0 0 0 15.6 0a7.8 7.8 0 0 0 15.6 0a7.8 7.8 0 0 0 15.6 0a7.8 7.8 0 0 0 15.6 0"/>
  <path d="M67.2 56v10M98.4 56v10M129.6 56v10M160.8 56v10M192 56v10"/>
  <rect x="56" y="92" width="46" height="40" rx="2"/><path d="M56 120h46"/><rect x="158" y="92" width="46" height="40" rx="2"/><path d="M158 120h46"/>
  <rect x="114" y="94" width="32" height="66" rx="2"/><rect x="120" y="100" width="20" height="22" rx="1.5"/><circle cx="140" cy="132" r="1.6" fill="currentColor"/>
  <path d="M130 84v6M126 90h8l-1.5 4h-5z"/>${ARTE[tipo]}</svg>`;
}

/** Sello circular de la portada: texto en círculo y la inicial del negocio. */
export function selloSvg(texto: string, letra: string): string {
  const t = texto.toUpperCase();
  const anillo = t.length > 20 ? `${t} · ` : `${t} · ${t} · `;
  return `<svg class="stamp" viewBox="0 0 140 140" aria-hidden="true"><defs><path id="stamp-ring" d="M70 70m-52 0a52 52 0 1 1 104 0a52 52 0 1 1-104 0"/></defs>
    <circle cx="70" cy="70" r="67" fill="rgba(21,18,13,.35)" stroke="#F1E9DC" stroke-width="1.6"/>
    <text fill="#F1E9DC" font-family="Onest,sans-serif" font-size="11" font-weight="600"><textPath href="#stamp-ring" textLength="322" lengthAdjust="spacing">${esc(anillo)}</textPath></text>
    <text x="70" y="86" text-anchor="middle" fill="#fff" font-family="'Playfair Display',Georgia,serif" font-size="46" font-weight="700">${esc(letra)}</text></svg>`;
}

export const OLA = '<svg class="wave" viewBox="0 0 1440 40" preserveAspectRatio="none" aria-hidden="true"><path d="M0 22C180 6 360 34 560 20S920 2 1120 16 1360 30 1440 18V40H0Z"/></svg>';
export const FLECHA_NOTA = '<svg class="note-arrow" viewBox="0 0 60 50" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M8 3c-3 17 6 32 33 38"/><path d="M29 33l12 8-12 4"/></svg>';
