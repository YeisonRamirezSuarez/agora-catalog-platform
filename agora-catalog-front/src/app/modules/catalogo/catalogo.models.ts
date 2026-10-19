/** Respuesta de GET /public/catalogos/{hash}. Con precios ocultos no llegan los campos de precio. */
export interface CatalogoPublico {
  negocio: Negocio;
  whatsapps: Whatsapp[];
  sellos: Sello[];
  horario: Horario[];
  /** Zona del negocio: "abierto ahora" se calcula con su hora, no con la del visitante. */
  zonaHoraria: string;
  categorias: Categoria[];
  franja: Franja | null;
  colecciones: Coleccion[];
  productos: Producto[];
}

export interface Negocio {
  nombre: string;
  nombreCorto: string | null;
  lema: string | null;
  slug: string;
  publicHash: string;
  logoUrl: string | null;
  portadaUrl: string | null;
  colorPrimario: string;
  colorSecundario: string;
  portadaAntetitulo: string | null;
  portadaTitulo: string | null;
  portadaTituloAcento: string | null;
  portadaSubtitulo: string | null;
  portadaNota: string | null;
  portadaSelloTexto: string | null;
  catalogoTitulo: string | null;
  catalogoAcento: string | null;
  catalogoSubtitulo: string | null;
  coleccionesTitulo: string;
  coleccionesTexto: string | null;
  coleccionesNota: string | null;
  visitaNota: string | null;
  fachada: Fachada | null;
  adornos: boolean;
  direccion: string | null;
  horario: string | null;
  infoDomicilios: string | null;
  domicilioActivo: boolean;
  /** null: el negocio confirma el costo por WhatsApp; 0: gratis. */
  domicilioCosto: number | null;
  domicilioMinimo: number | null;
  domicilioGratisDesde: number | null;
  mostrarPrecios: boolean;
  pedidosWhatsapp: boolean;
}

export type Fachada = 'SALSAMENTARIA' | 'PANADERIA' | 'FRUTERIA' | 'CAFETERIA' | 'MINIMERCADO' | 'FERRETERIA';
export type IconoSello = 'CALIDAD' | 'DOMICILIO' | 'WHATSAPP' | 'PRECIO' | 'TRADICION' | 'HORARIO';
export type UnidadVenta = 'UNIDAD' | 'LIBRA' | 'KILO' | 'GRAMOS' | 'PAQUETE' | 'CAJA';

export interface Whatsapp {
  numero: string;
  nombre: string | null;
}

export interface Sello {
  icono: IconoSello;
  texto: string;
}

/** dia 1 = lunes ... 7 = domingo; horas "HH:mm". El día que no viene, no abre. */
export interface Horario {
  dia: number;
  abre: string;
  cierra: string;
}

export interface Categoria {
  id: number;
  nombre: string;
  imagenUrl: string | null;
}

export interface Franja {
  categoriaId: number;
  titulo: string | null;
  frase: string | null;
  nota: string | null;
  imagenUrl: string | null;
}

export interface Coleccion {
  id: number;
  nombre: string;
  descripcion: string | null;
  imagenUrl: string | null;
  productos: number[];
}

export interface Producto {
  id: number;
  categoriaId: number;
  nombre: string;
  descripcion: string | null;
  presentacion: string | null;
  unidadVenta: UnidadVenta;
  precio?: number;
  precioOferta?: number;
  precioFinal?: number;
  ofertaVigente?: boolean;
  descuento?: number;
  imagenUrl: string | null;
  imagenThumbUrl: string | null;
  disponible: boolean;
  destacado: boolean;
  nuevo: boolean;
}

/** Una hoja (modal) del catálogo abierta. */
export type Hoja =
  | {tipo: 'producto'; id: number}
  | {tipo: 'pedido'}
  | {tipo: 'menu'}
  | {tipo: 'filtros'}
  | {tipo: 'whatsapp'; mensaje: string; pedido?: boolean};

/** Filtros de la página de catálogo; viven en la URL para poder compartir una búsqueda. */
export interface Filtros {
  cats: number[];
  promo: boolean;
  mas: boolean;
  disp: boolean;
  favs: boolean;
  col: number;
  q: string;
  min: number;
  max: number;
  orden: 'rel' | 'menor' | 'mayor' | 'az';
  lista: boolean;
}
