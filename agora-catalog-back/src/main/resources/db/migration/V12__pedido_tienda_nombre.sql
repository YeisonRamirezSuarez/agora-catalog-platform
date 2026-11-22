-- ============================================================
-- V12 — la tienda del pedido es solo un nombre
-- ============================================================
-- QUE HACE:    1 ALTER TABLE ADD COLUMN (aditivo), 1 UPDATE que llena la columna nueva con el nombre
--              de la tienda (estaba vacía: no pisa datos), SET NOT NULL en la columna nueva,
--              DROP NOT NULL en tienda_id (relaja una restricción) y 1 índice
-- DESTRUCTIVO: NO (la tabla tienda y sus filas se conservan; los pedidos viejos siguen ligados a ella)
-- TIPOS:       no cambia el tipo de ninguna columna
-- POR QUE:     el dueño no quiere llevar un listado de tiendas: en el pedido escribe el nombre y ya.
--              El nombre queda guardado en el pedido (para el comprobante y el tablero de rutas) y
--              no se crea ninguna tienda. Los nombres ya usados se sugieren desde los pedidos.
-- MEDIDO EN:   local — 13 pedidos con tienda_nombre llenado; 3 tiendas existentes intactas
-- ============================================================

ALTER TABLE main.pedido_tienda ADD COLUMN IF NOT EXISTS tienda_nombre VARCHAR(80);

UPDATE main.pedido_tienda p
   SET tienda_nombre = t.nombre
  FROM main.tienda t
 WHERE t.id = p.tienda_id AND p.tienda_nombre IS NULL;

ALTER TABLE main.pedido_tienda ALTER COLUMN tienda_nombre SET NOT NULL;
ALTER TABLE main.pedido_tienda ALTER COLUMN tienda_id DROP NOT NULL;

CREATE INDEX IF NOT EXISTS ix_pedido_tienda_nombre ON main.pedido_tienda (empresa_id, lower(tienda_nombre));
CREATE INDEX IF NOT EXISTS ix_pedido_tienda_fecha ON main.pedido_tienda (empresa_id, created_at);
