/**
 * Paleta y tipografías del sistema de diseño (DESIGN.md en la raíz).
 *
 * `brand` y `accent` son los dos colores que elige cada negocio: llegan como variables CSS
 * (`--brand`, `--accent`) que el catálogo fija al cargar la empresa. El resto es fijo.
 *
 * @type {import('tailwindcss').Config}
 */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  theme: {
    extend: {
      colors: {
        brand: 'var(--brand)',
        accent: 'var(--accent)',
        'on-accent': '#231A0E',
        price: '#B4182D',
        night: {DEFAULT: '#15120D', text: '#ECE4D6', muted: '#D8CEBF'},
        cream: {DEFAULT: '#F7F1E6', ink: '#211C16', 'ink-soft': '#4A4036', muted: '#6F655A', line: '#E7DDCC'},
        sand: '#EFE4D0',
        paper: '#FFFDF9',
        ink: {DEFAULT: '#1C1814', hover: '#342C24', soft: '#3D362F'},
        muted: '#6B6259',
        faint: '#776D63',
        line: {DEFAULT: '#ECE6DC', strong: '#DCD3C6'},
        wash: '#F6F2EA',
        'agora-gold': '#E0A43A',
        whatsapp: {DEFAULT: '#138A4A', press: '#0E733D'},
        danger: {DEFAULT: '#C42B2B', soft: '#FCEDED', ink: '#7A1C1C'},
        ok: {DEFAULT: '#1E7A46', soft: '#E8F4EC', ink: '#165C35'},
        warn: {soft: '#FFF6DB', ink: '#6A4B00'},
      },
      fontFamily: {
        display: ['"Playfair Display"', 'Georgia', '"Times New Roman"', 'serif'],
        hand: ['"Ms Madi"', 'cursive'],
        sans: ['Onest', 'system-ui', '-apple-system', '"Segoe UI"', 'Roboto', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
