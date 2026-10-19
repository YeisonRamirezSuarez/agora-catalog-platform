-- ============================================================
-- V2 — proveedor de correo (SMTP) y bitácora de envíos
-- ============================================================
-- QUE HACE:    2 CREATE TABLE (aditivo)
-- DESTRUCTIVO: NO
-- TIPOS:       no cambia el tipo de ninguna columna (tablas nuevas)
-- POR QUE:     enviar al administrador de un negocio nuevo su usuario y contraseña, y dejar
--              registro de cada correo (base para campañas a futuro)
-- MEDIDO EN:   local — 0 filas afectadas
-- ============================================================

-- Una sola fila: la plataforma tiene un proveedor. id fijo en 1 para que no haya dos.
CREATE TABLE IF NOT EXISTS main.proveedor_correo (
    id                 SMALLINT     PRIMARY KEY DEFAULT 1,
    host               VARCHAR(160) NOT NULL,
    puerto             INT          NOT NULL,
    usuario            VARCHAR(160),
    -- Cifrada con AES (AES_KEY): la base nunca guarda la contraseña en claro.
    password_cifrada   VARCHAR(500),
    remitente_email    VARCHAR(160) NOT NULL,
    remitente_nombre   VARCHAR(120) NOT NULL,
    -- STARTTLS (puerto 587) o SSL directo (puerto 465).
    seguridad          VARCHAR(10)  NOT NULL DEFAULT 'STARTTLS',
    activo             BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_proveedor_correo_unico CHECK (id = 1),
    CONSTRAINT ck_proveedor_correo_puerto CHECK (puerto BETWEEN 1 AND 65535),
    CONSTRAINT ck_proveedor_correo_seguridad CHECK (seguridad IN ('STARTTLS', 'SSL', 'NINGUNA'))
);

CREATE TABLE IF NOT EXISTS main.correo_envio (
    id            BIGSERIAL    PRIMARY KEY,
    -- Para filtrar los envíos de un negocio; NULL en los de la plataforma (prueba del proveedor).
    empresa_id    BIGINT       REFERENCES main.empresa (id) ON DELETE SET NULL,
    destinatario  VARCHAR(160) NOT NULL,
    asunto        VARCHAR(200) NOT NULL,
    -- Qué plantilla se usó: BIENVENIDA, PRUEBA, ...
    tipo          VARCHAR(30)  NOT NULL,
    enviado       BOOLEAN      NOT NULL,
    error         VARCHAR(1000),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_correo_envio_fecha ON main.correo_envio (created_at DESC);
