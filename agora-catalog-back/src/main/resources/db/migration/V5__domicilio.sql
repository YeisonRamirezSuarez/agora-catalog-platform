-- ============================================================
-- V5 — domicilio: costo de envío, pedido mínimo y envío gratis
-- ============================================================
-- QUE HACE:    1 ALTER TABLE con 4 ADD COLUMN (aditivo)
-- DESTRUCTIVO: NO
-- TIPOS:       no cambia el tipo de ninguna columna (columnas nuevas)
-- POR QUE:     el domicilio era solo texto: el pedido llegaba sin el costo del envío y por debajo
--              del mínimo, y el negocio tenía que aclararlo a mano por WhatsApp.
-- MEDIDO EN:   local — 0 filas afectadas (las empresas quedan sin domicilio configurado)
-- ============================================================

-- Pesos colombianos sin decimales. Costo NULL: el negocio lo confirma por WhatsApp; 0: gratis.
ALTER TABLE main.empresa
    ADD COLUMN IF NOT EXISTS domicilio_activo       BOOLEAN       NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS domicilio_costo        NUMERIC(12,0) CHECK (domicilio_costo >= 0),
    ADD COLUMN IF NOT EXISTS domicilio_minimo       NUMERIC(12,0) CHECK (domicilio_minimo > 0),
    ADD COLUMN IF NOT EXISTS domicilio_gratis_desde NUMERIC(12,0) CHECK (domicilio_gratis_desde > 0);
