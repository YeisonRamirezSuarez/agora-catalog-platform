-- ============================================================
-- V10 — fecha de la última edición de un pedido a tienda
-- ============================================================
-- QUE HACE:    1 ALTER TABLE ADD COLUMN (aditivo)
-- DESTRUCTIVO: NO
-- TIPOS:       no cambia el tipo de ninguna columna
-- POR QUE:     un pedido se puede corregir después (la tienda pidió otra cantidad, hubo un error al
--              anotarlo). El comprobante dice que fue editado y cuándo, para que no haya dudas
--              entre la copia vieja y la nueva.
-- MEDIDO EN:   local — 0 filas afectadas
-- ============================================================

ALTER TABLE main.pedido_tienda ADD COLUMN IF NOT EXISTS editado_at TIMESTAMPTZ;
