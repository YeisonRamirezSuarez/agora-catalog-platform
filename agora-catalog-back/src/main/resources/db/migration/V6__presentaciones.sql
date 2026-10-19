-- ============================================================
-- V6 — otras presentaciones de un producto (media libra, tajado...)
-- ============================================================
-- QUE HACE:    1 CREATE TABLE (aditivo), 2 CREATE INDEX
-- DESTRUCTIVO: NO
-- TIPOS:       no cambia el tipo de ninguna columna (tabla nueva)
-- POR QUE:     un producto se vende en varias presentaciones con precio distinto (libra y media
--              libra) y hoy había que crearlo dos veces. La presentación principal sigue siendo la
--              del producto (presentacion y precio): Excel, filtros y orden por precio no cambian.
-- MEDIDO EN:   local — 0 filas afectadas
-- ============================================================

CREATE TABLE IF NOT EXISTS main.producto_presentacion (
    id             BIGSERIAL     PRIMARY KEY,
    empresa_id     BIGINT        NOT NULL REFERENCES main.empresa (id) ON DELETE CASCADE,
    -- El servicio valida que el producto sea de la misma empresa antes de guardar.
    producto_id    BIGINT        NOT NULL REFERENCES main.producto (id) ON DELETE CASCADE,
    nombre         VARCHAR(60)   NOT NULL,
    -- Pesos colombianos sin decimales. La oferta usa las fechas de oferta del producto.
    precio         NUMERIC(12,0) NOT NULL,
    precio_oferta  NUMERIC(12,0),
    orden          INT           NOT NULL DEFAULT 0,
    CONSTRAINT ck_presentacion_precio CHECK (precio >= 0),
    CONSTRAINT ck_presentacion_oferta CHECK (precio_oferta IS NULL OR (precio_oferta >= 0 AND precio_oferta < precio))
);

CREATE INDEX IF NOT EXISTS ix_producto_presentacion_empresa ON main.producto_presentacion (empresa_id, producto_id, orden);
-- El carrito del visitante guarda la presentación por su nombre: no se repite en un mismo producto.
CREATE UNIQUE INDEX IF NOT EXISTS uq_producto_presentacion_nombre ON main.producto_presentacion (producto_id, lower(nombre));
