-- ============================================================
-- V8 — módulos que cada empresa tiene contratados
-- ============================================================
-- QUE HACE:    1 CREATE TABLE (aditivo); INSERT con ON CONFLICT DO NOTHING de los módulos que cada
--              empresa existente ya usaba
-- DESTRUCTIVO: NO
-- TIPOS:       no cambia el tipo de ninguna columna (tabla nueva)
-- POR QUE:     la plataforma se vende por módulos: un negocio vende tienda a tienda y otro solo
--              quiere el catálogo. El super admin decide qué tiene cada uno. Sin fila, el módulo
--              está apagado; el catálogo (productos, categorías, configuración, link público) no es
--              un módulo: lo tienen todas.
--              Las empresas que ya existen conservan lo que usaban hasta hoy (pedido por WhatsApp,
--              importar, colecciones y estadísticas) y la venta en ruta solo si ya tienen carros.
-- MEDIDO EN:   local — 1 empresa (con carros), 5 filas insertadas
-- ============================================================

CREATE TABLE IF NOT EXISTS main.empresa_modulo (
    empresa_id  BIGINT       NOT NULL REFERENCES main.empresa (id) ON DELETE CASCADE,
    modulo      VARCHAR(30)  NOT NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_empresa_modulo PRIMARY KEY (empresa_id, modulo),
    CONSTRAINT ck_empresa_modulo CHECK (modulo IN ('PEDIDO_WHATSAPP', 'IMPORTAR_EXCEL', 'COLECCIONES', 'ESTADISTICAS', 'VENTA_RUTA'))
);

INSERT INTO main.empresa_modulo (empresa_id, modulo)
SELECT e.id, m.modulo
  FROM main.empresa e
 CROSS JOIN (VALUES ('PEDIDO_WHATSAPP'), ('IMPORTAR_EXCEL'), ('COLECCIONES'), ('ESTADISTICAS')) AS m (modulo)
ON CONFLICT (empresa_id, modulo) DO NOTHING;

INSERT INTO main.empresa_modulo (empresa_id, modulo)
SELECT DISTINCT c.empresa_id, 'VENTA_RUTA' FROM main.carro c
ON CONFLICT (empresa_id, modulo) DO NOTHING;
