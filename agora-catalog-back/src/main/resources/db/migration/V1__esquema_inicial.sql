-- ============================================================
-- V1 — esquema inicial: empresas, usuarios, catálogo
-- ============================================================
-- QUE HACE:    11 CREATE TABLE (aditivo) con sus índices y restricciones
-- DESTRUCTIVO: NO
-- TIPOS:       no cambia el tipo de ninguna columna (tablas nuevas)
-- POR QUE:     modelo de datos de ESPECIFICACION.md §4; toda tabla de negocio lleva empresa_id
-- MEDIDO EN:   local — base vacía, 0 filas afectadas
-- ============================================================

CREATE TABLE IF NOT EXISTS main.empresa (
    id                     BIGSERIAL PRIMARY KEY,
    nombre                 VARCHAR(120) NOT NULL,
    -- Editable; el link público se resuelve por public_hash, así que cambiarlo no rompe links viejos.
    slug                   VARCHAR(80)  NOT NULL,
    -- Inmutable: 8 caracteres [a-z0-9] generados con SecureRandom.
    public_hash            CHAR(8)      NOT NULL,
    logo_url               VARCHAR(300),
    portada_url            VARCHAR(300),
    logo_original_url      VARCHAR(300),
    logo_recorte           JSONB,
    portada_original_url   VARCHAR(300),
    portada_recorte        JSONB,
    color_primario         CHAR(7)      NOT NULL DEFAULT '#24331F',
    color_secundario       CHAR(7)      NOT NULL DEFAULT '#DDB06C',
    nombre_corto           VARCHAR(24),
    lema                   VARCHAR(32),
    portada_antetitulo     VARCHAR(40),
    portada_titulo         VARCHAR(40),
    portada_titulo_acento  VARCHAR(40),
    portada_subtitulo      VARCHAR(140),
    portada_nota           VARCHAR(48),
    portada_sello_texto    VARCHAR(30),
    catalogo_titulo        VARCHAR(40),
    catalogo_acento        VARCHAR(30),
    catalogo_subtitulo     VARCHAR(140),
    colecciones_titulo     VARCHAR(40)  NOT NULL DEFAULT '¿Cuál es el plan?',
    colecciones_texto      VARCHAR(80),
    colecciones_nota       VARCHAR(40),
    visita_nota            VARCHAR(40),
    fachada                VARCHAR(20),
    adornos                BOOLEAN      NOT NULL DEFAULT TRUE,
    direccion              VARCHAR(200),
    horario                VARCHAR(300),
    info_domicilios        VARCHAR(300),
    mostrar_precios        BOOLEAN      NOT NULL DEFAULT TRUE,
    pedidos_whatsapp       BOOLEAN      NOT NULL DEFAULT TRUE,
    mostrar_agotados       BOOLEAN      NOT NULL DEFAULT TRUE,
    activa                 BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_empresa_slug UNIQUE (slug),
    CONSTRAINT uq_empresa_public_hash UNIQUE (public_hash),
    CONSTRAINT ck_empresa_slug CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    CONSTRAINT ck_empresa_public_hash CHECK (public_hash ~ '^[a-z0-9]{8}$'),
    CONSTRAINT ck_empresa_colores CHECK (color_primario ~ '^#[0-9A-Fa-f]{6}$' AND color_secundario ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT ck_empresa_fachada CHECK (fachada IN ('SALSAMENTARIA', 'PANADERIA', 'FRUTERIA', 'CAFETERIA', 'MINIMERCADO', 'FERRETERIA'))
);

CREATE TABLE IF NOT EXISTS main.empresa_sello (
    id          BIGSERIAL PRIMARY KEY,
    empresa_id  BIGINT      NOT NULL REFERENCES main.empresa (id) ON DELETE CASCADE,
    icono       VARCHAR(20) NOT NULL,
    texto       VARCHAR(48) NOT NULL,
    orden       INT         NOT NULL DEFAULT 0,
    CONSTRAINT ck_empresa_sello_icono CHECK (icono IN ('CALIDAD', 'DOMICILIO', 'WHATSAPP', 'PRECIO', 'TRADICION', 'HORARIO'))
);
CREATE INDEX IF NOT EXISTS ix_empresa_sello_empresa ON main.empresa_sello (empresa_id, orden);

CREATE TABLE IF NOT EXISTS main.empresa_whatsapp (
    id          BIGSERIAL PRIMARY KEY,
    empresa_id  BIGINT      NOT NULL REFERENCES main.empresa (id) ON DELETE CASCADE,
    -- Formato internacional, solo dígitos (573163218276).
    numero      VARCHAR(15) NOT NULL,
    nombre      VARCHAR(40),
    orden       INT         NOT NULL DEFAULT 0,
    CONSTRAINT uq_empresa_whatsapp UNIQUE (empresa_id, numero),
    CONSTRAINT ck_empresa_whatsapp_numero CHECK (numero ~ '^[0-9]{10,15}$')
);

CREATE TABLE IF NOT EXISTS main.usuario (
    id                     BIGSERIAL PRIMARY KEY,
    -- NULL solo para SUPER_ADMIN.
    empresa_id             BIGINT       REFERENCES main.empresa (id),
    nombre                 VARCHAR(120) NOT NULL,
    email                  VARCHAR(160) NOT NULL,
    password_hash          VARCHAR(100) NOT NULL,
    rol                    VARCHAR(20)  NOT NULL,
    activo                 BOOLEAN      NOT NULL DEFAULT TRUE,
    -- Contraseña temporal: el login pide cambiarla antes de entrar al panel.
    debe_cambiar_password  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_usuario_rol CHECK (
        (rol = 'SUPER_ADMIN' AND empresa_id IS NULL) OR (rol = 'ADMIN_EMPRESA' AND empresa_id IS NOT NULL))
);
-- El correo se compara sin mayúsculas: Javier@x.com y javier@x.com son la misma cuenta.
CREATE UNIQUE INDEX IF NOT EXISTS uq_usuario_email ON main.usuario (lower(email));
CREATE INDEX IF NOT EXISTS ix_usuario_empresa ON main.usuario (empresa_id);

CREATE TABLE IF NOT EXISTS main.refresh_token (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT      NOT NULL REFERENCES main.usuario (id) ON DELETE CASCADE,
    -- SHA-256 en hex del token: la base nunca guarda el token en claro.
    token_hash  CHAR(64)    NOT NULL,
    expira_en   TIMESTAMPTZ NOT NULL,
    revocado    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);
CREATE INDEX IF NOT EXISTS ix_refresh_token_usuario ON main.refresh_token (usuario_id) WHERE NOT revocado;

CREATE TABLE IF NOT EXISTS main.categoria (
    id             BIGSERIAL PRIMARY KEY,
    empresa_id     BIGINT      NOT NULL REFERENCES main.empresa (id) ON DELETE CASCADE,
    nombre         VARCHAR(60) NOT NULL,
    orden          INT         NOT NULL DEFAULT 0,
    activa         BOOLEAN     NOT NULL DEFAULT TRUE,
    imagen_url     VARCHAR(300),
    -- Franja grande del inicio del catálogo.
    banner_activo  BOOLEAN     NOT NULL DEFAULT FALSE,
    banner_url     VARCHAR(300),
    banner_titulo  VARCHAR(40),
    banner_frase   VARCHAR(80),
    banner_nota    VARCHAR(32),
    CONSTRAINT uq_categoria_nombre UNIQUE (empresa_id, nombre),
    -- Destino de la FK compuesta de producto: la categoría de un producto es de su misma empresa.
    CONSTRAINT uq_categoria_id_empresa UNIQUE (id, empresa_id)
);
-- Una sola categoría con franja por empresa.
CREATE UNIQUE INDEX IF NOT EXISTS uq_categoria_banner ON main.categoria (empresa_id) WHERE banner_activo;

CREATE TABLE IF NOT EXISTS main.producto (
    id                BIGSERIAL PRIMARY KEY,
    empresa_id        BIGINT        NOT NULL REFERENCES main.empresa (id) ON DELETE CASCADE,
    categoria_id      BIGINT        NOT NULL,
    -- Clave para Excel y para asignar imágenes del ZIP.
    codigo            VARCHAR(40)   NOT NULL,
    nombre            VARCHAR(120)  NOT NULL,
    descripcion       VARCHAR(500),
    presentacion      VARCHAR(60),
    unidad_venta      VARCHAR(10)   NOT NULL,
    -- Pesos colombianos sin decimales.
    precio            NUMERIC(12,0) NOT NULL,
    precio_oferta     NUMERIC(12,0),
    oferta_desde      DATE,
    oferta_hasta      DATE,
    imagen_url        VARCHAR(300),
    imagen_thumb_url  VARCHAR(300),
    disponible        BOOLEAN       NOT NULL DEFAULT TRUE,
    destacado         BOOLEAN       NOT NULL DEFAULT FALSE,
    orden             INT           NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_producto_codigo UNIQUE (empresa_id, codigo),
    CONSTRAINT fk_producto_categoria FOREIGN KEY (categoria_id, empresa_id)
        REFERENCES main.categoria (id, empresa_id),
    CONSTRAINT ck_producto_unidad CHECK (unidad_venta IN ('UNIDAD', 'LIBRA', 'KILO', 'GRAMOS', 'PAQUETE', 'CAJA')),
    CONSTRAINT ck_producto_precio CHECK (precio >= 0),
    CONSTRAINT ck_producto_oferta CHECK (precio_oferta IS NULL OR (precio_oferta >= 0 AND precio_oferta < precio)),
    CONSTRAINT ck_producto_fechas_oferta CHECK (oferta_desde IS NULL OR oferta_hasta IS NULL OR oferta_desde <= oferta_hasta)
);
CREATE INDEX IF NOT EXISTS ix_producto_empresa_categoria ON main.producto (empresa_id, categoria_id);

CREATE TABLE IF NOT EXISTS main.coleccion (
    id           BIGSERIAL PRIMARY KEY,
    empresa_id   BIGINT      NOT NULL REFERENCES main.empresa (id) ON DELETE CASCADE,
    nombre       VARCHAR(40) NOT NULL,
    descripcion  VARCHAR(80),
    imagen_url   VARCHAR(300),
    visible      BOOLEAN     NOT NULL DEFAULT TRUE,
    orden        INT         NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS ix_coleccion_empresa ON main.coleccion (empresa_id, orden);

CREATE TABLE IF NOT EXISTS main.coleccion_producto (
    coleccion_id  BIGINT NOT NULL REFERENCES main.coleccion (id) ON DELETE CASCADE,
    -- Borrar un producto lo quita de sus colecciones.
    producto_id   BIGINT NOT NULL REFERENCES main.producto (id) ON DELETE CASCADE,
    orden         INT    NOT NULL DEFAULT 0,
    PRIMARY KEY (coleccion_id, producto_id)
);
CREATE INDEX IF NOT EXISTS ix_coleccion_producto_producto ON main.coleccion_producto (producto_id);
