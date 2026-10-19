-- ============================================================
-- V4 — horario de atención por día
-- ============================================================
-- QUE HACE:    1 CREATE TABLE (aditivo)
-- DESTRUCTIVO: NO
-- TIPOS:       no cambia el tipo de ninguna columna (tabla nueva)
-- POR QUE:     el horario era solo texto libre: el catálogo no podía decir si el negocio está
--              abierto ahora ni avisar a quien pide fuera de horario. El texto de empresa.horario
--              queda para aclaraciones (festivos).
-- MEDIDO EN:   local — 0 filas afectadas
-- ============================================================

-- Un tramo por día; el día que no tiene fila, el negocio no abre. Sin filas: horario sin configurar.
CREATE TABLE IF NOT EXISTS main.empresa_horario (
    empresa_id  BIGINT   NOT NULL REFERENCES main.empresa (id) ON DELETE CASCADE,
    -- ISO 8601: 1 = lunes ... 7 = domingo.
    dia         SMALLINT NOT NULL,
    abre        TIME     NOT NULL,
    cierra      TIME     NOT NULL,
    CONSTRAINT pk_empresa_horario PRIMARY KEY (empresa_id, dia),
    CONSTRAINT ck_empresa_horario_dia CHECK (dia BETWEEN 1 AND 7),
    -- Sin turnos que pasen de medianoche.
    CONSTRAINT ck_empresa_horario_tramo CHECK (abre < cierra)
);
