-- ============================================================
-- V9 — eliminar un carro con historial sin perder sus pedidos
-- ============================================================
-- QUE HACE:    1 ALTER TABLE ADD COLUMN (aditivo); cambia el índice único del nombre del carro por
--              uno que solo cuenta los carros no eliminados (DROP INDEX + CREATE UNIQUE INDEX)
-- DESTRUCTIVO: NO (quita un índice, no datos; el nuevo es igual de estricto entre carros vigentes)
-- TIPOS:       no cambia el tipo de ninguna columna
-- POR QUE:     el negocio quiere eliminar carros que ya no usa. Uno con movimientos o pedidos no se
--              puede borrar de verdad sin dejar esos pedidos sin carro, así que se marca como
--              eliminado: desaparece del panel y los comprobantes siguen mostrando de qué carro salió.
--              El nombre queda libre para un carro nuevo.
-- MEDIDO EN:   local — 0 filas afectadas
-- ============================================================

ALTER TABLE main.carro ADD COLUMN IF NOT EXISTS eliminado_at TIMESTAMPTZ;

DROP INDEX IF EXISTS main.uq_carro_nombre;
CREATE UNIQUE INDEX IF NOT EXISTS uq_carro_nombre_vigente ON main.carro (empresa_id, lower(nombre)) WHERE eliminado_at IS NULL;
