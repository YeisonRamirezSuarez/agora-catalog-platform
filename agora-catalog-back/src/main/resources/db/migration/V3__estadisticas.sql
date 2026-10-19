-- ============================================================
-- V3 — estadísticas diarias del catálogo público
-- ============================================================
-- QUE HACE:    1 CREATE TABLE (aditivo), 1 CREATE UNIQUE INDEX
-- DESTRUCTIVO: NO
-- TIPOS:       no cambia el tipo de ninguna columna (tabla nueva)
-- POR QUE:     el dueño no sabe si su catálogo le sirve: cuántas visitas tiene, qué productos
--              miran y cuántos pedidos salen a WhatsApp. Solo contadores por día, sin datos de
--              quien visita.
-- MEDIDO EN:   local — 0 filas afectadas
-- ============================================================

CREATE TABLE IF NOT EXISTS main.estadistica_diaria (
    empresa_id   BIGINT      NOT NULL REFERENCES main.empresa (id) ON DELETE CASCADE,
    -- Día en America/Bogota (el Clock del backend).
    fecha        DATE        NOT NULL,
    -- VISITA: abrir el catálogo (una por sesión del navegador). PRODUCTO: abrir la hoja de un
    -- producto. PEDIDO: enviar el pedido a WhatsApp. WHATSAPP: cualquier otro clic a WhatsApp.
    tipo         VARCHAR(12) NOT NULL,
    -- Solo en PRODUCTO. Borrar el producto borra sus vistas.
    producto_id  BIGINT      REFERENCES main.producto (id) ON DELETE CASCADE,
    cantidad     INT         NOT NULL DEFAULT 0,
    CONSTRAINT ck_estadistica_tipo CHECK (tipo IN ('VISITA', 'PRODUCTO', 'PEDIDO', 'WHATSAPP')),
    CONSTRAINT ck_estadistica_producto CHECK ((tipo = 'PRODUCTO') = (producto_id IS NOT NULL)),
    CONSTRAINT ck_estadistica_cantidad CHECK (cantidad >= 0)
);

-- Una fila por empresa, día, tipo y producto: cada evento suma 1 con ON CONFLICT. Empieza por
-- empresa_id, que es por donde se consulta siempre.
CREATE UNIQUE INDEX IF NOT EXISTS uq_estadistica_diaria
    ON main.estadistica_diaria (empresa_id, fecha, tipo, (COALESCE(producto_id, 0)));
