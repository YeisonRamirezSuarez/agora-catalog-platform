-- ============================================================
-- V11 — rutas (salidas) de un carro
-- ============================================================
-- QUE HACE:    1 CREATE TABLE + 2 índices, 1 ALTER TABLE ADD COLUMN, se amplía el CHECK de tipos de
--              movimiento (se agrega INICIAL), 1 INSERT (una ruta por carro con historial) y 1 UPDATE
--              que llena la columna nueva ruta_id (estaba vacía: no pisa datos)
-- DESTRUCTIVO: NO
-- TIPOS:       no cambia el tipo de ninguna columna
-- POR QUE:     un carro sale varias veces (7 a. m. una ruta, 11 a. m. otra, otro día otra). El dueño
--              quiere ver por fecha qué cargó, qué vendió y qué le quedó en cada salida, y al empezar
--              una ruta nueva decidir si lo que sobró de la anterior sigue en el carro o se baja.
--              INICIAL es lo que la ruta nueva trae de la anterior (no es una carga nueva).
-- MEDIDO EN:   local — 5 carros con historial → 5 rutas; 48 movimientos con ruta_id llenado
-- ============================================================

CREATE TABLE IF NOT EXISTS main.carro_ruta (
    id           BIGSERIAL    PRIMARY KEY,
    empresa_id   BIGINT       NOT NULL REFERENCES main.empresa (id) ON DELETE CASCADE,
    carro_id     BIGINT       NOT NULL,
    iniciada_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- NULL: es la ruta en curso del carro (hay a lo sumo una).
    cerrada_at   TIMESTAMPTZ,
    usuario_id   BIGINT       REFERENCES main.usuario (id) ON DELETE SET NULL,
    CONSTRAINT fk_carro_ruta_carro FOREIGN KEY (carro_id, empresa_id)
        REFERENCES main.carro (id, empresa_id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_carro_ruta_en_curso ON main.carro_ruta (carro_id) WHERE cerrada_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_carro_ruta_carro ON main.carro_ruta (empresa_id, carro_id, iniciada_at DESC);

ALTER TABLE main.carro_movimiento ADD COLUMN IF NOT EXISTS ruta_id BIGINT
    REFERENCES main.carro_ruta (id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS ix_carro_movimiento_ruta ON main.carro_movimiento (ruta_id);

ALTER TABLE main.carro_movimiento DROP CONSTRAINT IF EXISTS ck_carro_movimiento_tipo;
ALTER TABLE main.carro_movimiento ADD CONSTRAINT ck_carro_movimiento_tipo
    CHECK (tipo IN ('CARGA', 'VENTA', 'AJUSTE', 'DESCARGUE', 'ANULACION', 'INICIAL'));

-- Lo que ya pasó en cada carro queda como su primera ruta, en curso.
INSERT INTO main.carro_ruta (empresa_id, carro_id, iniciada_at)
SELECT m.empresa_id, m.carro_id, min(m.created_at)
  FROM main.carro_movimiento m
 WHERE NOT EXISTS (SELECT 1 FROM main.carro_ruta r WHERE r.carro_id = m.carro_id)
 GROUP BY m.empresa_id, m.carro_id;

UPDATE main.carro_movimiento m
   SET ruta_id = r.id
  FROM main.carro_ruta r
 WHERE r.carro_id = m.carro_id AND r.cerrada_at IS NULL AND m.ruta_id IS NULL;
