-- ============================================================
-- V7 — venta en ruta: tiendas, carros con stock, pedidos a tiendas, listas de precios y abonos
-- ============================================================
-- QUE HACE:    8 CREATE TABLE (aditivo), sus índices y restricciones
-- DESTRUCTIVO: NO
-- TIPOS:       no cambia el tipo de ninguna columna (tablas nuevas)
-- POR QUE:     el negocio también vende tienda a tienda: carga uno o varios carros con producto,
--              recorre las tiendas, les deja un pedido con su comprobante y necesita saber cuánto
--              le queda en cada carro y qué le compró cada tienda. Algunas tiendas tienen precios
--              propios y a algunas se les fía. Las tablas de pedidos, listas de precios y abonos
--              se crean ya para no ir agregando columnas sueltas en cada fase.
-- MEDIDO EN:   local — 0 filas afectadas
-- ============================================================

-- Listas de precios: una tienda puede tener la suya; lo que la lista no trae sale del catálogo.
CREATE TABLE IF NOT EXISTS main.lista_precio (
    id          BIGSERIAL     PRIMARY KEY,
    empresa_id  BIGINT        NOT NULL REFERENCES main.empresa (id) ON DELETE CASCADE,
    nombre      VARCHAR(60)   NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_lista_precio_id_empresa UNIQUE (id, empresa_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_lista_precio_nombre ON main.lista_precio (empresa_id, lower(nombre));

CREATE TABLE IF NOT EXISTS main.lista_precio_item (
    id               BIGSERIAL     PRIMARY KEY,
    empresa_id       BIGINT        NOT NULL REFERENCES main.empresa (id) ON DELETE CASCADE,
    lista_precio_id  BIGINT        NOT NULL,
    -- El servicio valida que el producto sea de la misma empresa antes de guardar.
    producto_id      BIGINT        NOT NULL REFERENCES main.producto (id) ON DELETE CASCADE,
    -- Nombre de la otra presentación; NULL es la presentación principal del producto. Las
    -- presentaciones se reemplazan al guardar el producto, así que se referencian por nombre.
    presentacion     VARCHAR(60),
    -- Pesos colombianos sin decimales.
    precio           NUMERIC(12,0) NOT NULL,
    CONSTRAINT fk_lista_precio_item_lista FOREIGN KEY (lista_precio_id, empresa_id)
        REFERENCES main.lista_precio (id, empresa_id) ON DELETE CASCADE,
    CONSTRAINT ck_lista_precio_item_precio CHECK (precio >= 0)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_lista_precio_item
    ON main.lista_precio_item (lista_precio_id, producto_id, lower(COALESCE(presentacion, '')));
CREATE INDEX IF NOT EXISTS ix_lista_precio_item_empresa ON main.lista_precio_item (empresa_id, lista_precio_id);

CREATE TABLE IF NOT EXISTS main.tienda (
    id               BIGSERIAL     PRIMARY KEY,
    empresa_id       BIGINT        NOT NULL REFERENCES main.empresa (id) ON DELETE CASCADE,
    nombre           VARCHAR(80)   NOT NULL,
    encargado        VARCHAR(80),
    -- Solo dígitos, con indicativo si lo tiene; sirve para mandarle el comprobante por WhatsApp.
    telefono         VARCHAR(20),
    direccion        VARCHAR(150),
    barrio           VARCHAR(80),
    notas            VARCHAR(300),
    lista_precio_id  BIGINT,
    -- Pesos colombianos sin decimales. NULL: sin cupo definido (se le puede fiar sin aviso).
    cupo_credito     NUMERIC(12,0),
    activa           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_tienda_id_empresa UNIQUE (id, empresa_id),
    CONSTRAINT fk_tienda_lista_precio FOREIGN KEY (lista_precio_id, empresa_id)
        REFERENCES main.lista_precio (id, empresa_id),
    CONSTRAINT ck_tienda_cupo CHECK (cupo_credito IS NULL OR cupo_credito >= 0)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_tienda_nombre ON main.tienda (empresa_id, lower(nombre));

CREATE TABLE IF NOT EXISTS main.carro (
    id          BIGSERIAL     PRIMARY KEY,
    empresa_id  BIGINT        NOT NULL REFERENCES main.empresa (id) ON DELETE CASCADE,
    nombre      VARCHAR(60)   NOT NULL,
    placa       VARCHAR(15),
    activo      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_carro_id_empresa UNIQUE (id, empresa_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_carro_nombre ON main.carro (empresa_id, lower(nombre));

-- Lo que hay ahora en cada carro. Una fila que llega a cero se borra.
CREATE TABLE IF NOT EXISTS main.carro_stock (
    id           BIGSERIAL     PRIMARY KEY,
    empresa_id   BIGINT        NOT NULL REFERENCES main.empresa (id) ON DELETE CASCADE,
    carro_id     BIGINT        NOT NULL,
    -- Sin cascada: un producto que va en un carro no se puede borrar sin descargarlo antes.
    producto_id  BIGINT        NOT NULL REFERENCES main.producto (id),
    -- NULL: la presentación principal del producto.
    presentacion VARCHAR(60),
    -- Tres decimales: libras y kilos.
    cantidad     NUMERIC(12,3) NOT NULL,
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_carro_stock_carro FOREIGN KEY (carro_id, empresa_id)
        REFERENCES main.carro (id, empresa_id) ON DELETE CASCADE,
    CONSTRAINT ck_carro_stock_cantidad CHECK (cantidad > 0)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_carro_stock
    ON main.carro_stock (carro_id, producto_id, lower(COALESCE(presentacion, '')));
CREATE INDEX IF NOT EXISTS ix_carro_stock_empresa ON main.carro_stock (empresa_id, carro_id);
CREATE INDEX IF NOT EXISTS ix_carro_stock_producto ON main.carro_stock (producto_id);

CREATE TABLE IF NOT EXISTS main.pedido_tienda (
    id                BIGSERIAL     PRIMARY KEY,
    empresa_id        BIGINT        NOT NULL REFERENCES main.empresa (id) ON DELETE CASCADE,
    -- Consecutivo por empresa: el número que ve la tienda en el comprobante.
    numero            INT           NOT NULL,
    carro_id          BIGINT        NOT NULL,
    tienda_id         BIGINT        NOT NULL,
    forma_pago        VARCHAR(10)   NOT NULL DEFAULT 'CONTADO',
    -- Pesos colombianos sin decimales.
    total             NUMERIC(12,0) NOT NULL,
    saldo_pendiente   NUMERIC(12,0) NOT NULL DEFAULT 0,
    estado            VARCHAR(10)   NOT NULL DEFAULT 'CONFIRMADO',
    notas             VARCHAR(300),
    motivo_anulacion  VARCHAR(200),
    anulado_at        TIMESTAMPTZ,
    usuario_id        BIGINT        REFERENCES main.usuario (id) ON DELETE SET NULL,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_pedido_tienda_numero UNIQUE (empresa_id, numero),
    CONSTRAINT uq_pedido_tienda_id_empresa UNIQUE (id, empresa_id),
    CONSTRAINT fk_pedido_tienda_carro FOREIGN KEY (carro_id, empresa_id) REFERENCES main.carro (id, empresa_id),
    CONSTRAINT fk_pedido_tienda_tienda FOREIGN KEY (tienda_id, empresa_id) REFERENCES main.tienda (id, empresa_id),
    CONSTRAINT ck_pedido_tienda_forma_pago CHECK (forma_pago IN ('CONTADO', 'CREDITO')),
    CONSTRAINT ck_pedido_tienda_estado CHECK (estado IN ('CONFIRMADO', 'ANULADO')),
    CONSTRAINT ck_pedido_tienda_total CHECK (total >= 0),
    CONSTRAINT ck_pedido_tienda_saldo CHECK (saldo_pendiente >= 0 AND saldo_pendiente <= total)
);
CREATE INDEX IF NOT EXISTS ix_pedido_tienda_fecha ON main.pedido_tienda (empresa_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_pedido_tienda_tienda ON main.pedido_tienda (empresa_id, tienda_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_pedido_tienda_carro ON main.pedido_tienda (empresa_id, carro_id, created_at DESC);

-- Cada línea copia nombre y precio del momento de la venta: el historial no cambia con el catálogo.
CREATE TABLE IF NOT EXISTS main.pedido_tienda_item (
    id                BIGSERIAL     PRIMARY KEY,
    empresa_id        BIGINT        NOT NULL REFERENCES main.empresa (id) ON DELETE CASCADE,
    pedido_tienda_id  BIGINT        NOT NULL,
    producto_id       BIGINT        REFERENCES main.producto (id) ON DELETE SET NULL,
    producto_nombre   VARCHAR(120)  NOT NULL,
    presentacion      VARCHAR(60),
    unidad_venta      VARCHAR(10)   NOT NULL,
    cantidad          NUMERIC(12,3) NOT NULL,
    -- Pesos colombianos sin decimales. El sugerido es el de la lista de la tienda o el del catálogo.
    precio_sugerido   NUMERIC(12,0) NOT NULL,
    precio_unitario   NUMERIC(12,0) NOT NULL,
    subtotal          NUMERIC(12,0) NOT NULL,
    CONSTRAINT fk_pedido_tienda_item_pedido FOREIGN KEY (pedido_tienda_id, empresa_id)
        REFERENCES main.pedido_tienda (id, empresa_id) ON DELETE CASCADE,
    CONSTRAINT ck_pedido_tienda_item_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_pedido_tienda_item_precio CHECK (precio_unitario >= 0 AND precio_sugerido >= 0 AND subtotal >= 0)
);
CREATE INDEX IF NOT EXISTS ix_pedido_tienda_item_pedido ON main.pedido_tienda_item (empresa_id, pedido_tienda_id);

-- Cada entrada y salida de un carro, con el saldo que dejó: explica por qué el stock es el que es.
CREATE TABLE IF NOT EXISTS main.carro_movimiento (
    id                BIGSERIAL     PRIMARY KEY,
    empresa_id        BIGINT        NOT NULL REFERENCES main.empresa (id) ON DELETE CASCADE,
    carro_id          BIGINT        NOT NULL,
    producto_id       BIGINT        REFERENCES main.producto (id) ON DELETE SET NULL,
    producto_nombre   VARCHAR(120)  NOT NULL,
    presentacion      VARCHAR(60),
    tipo              VARCHAR(12)   NOT NULL,
    -- Positiva entra al carro, negativa sale.
    cantidad          NUMERIC(12,3) NOT NULL,
    saldo             NUMERIC(12,3) NOT NULL,
    motivo            VARCHAR(200),
    pedido_tienda_id  BIGINT,
    usuario_id        BIGINT        REFERENCES main.usuario (id) ON DELETE SET NULL,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_carro_movimiento_carro FOREIGN KEY (carro_id, empresa_id)
        REFERENCES main.carro (id, empresa_id) ON DELETE CASCADE,
    CONSTRAINT fk_carro_movimiento_pedido FOREIGN KEY (pedido_tienda_id, empresa_id)
        REFERENCES main.pedido_tienda (id, empresa_id),
    CONSTRAINT ck_carro_movimiento_tipo CHECK (tipo IN ('CARGA', 'VENTA', 'AJUSTE', 'DESCARGUE', 'ANULACION')),
    CONSTRAINT ck_carro_movimiento_cantidad CHECK (cantidad <> 0 AND saldo >= 0)
);
CREATE INDEX IF NOT EXISTS ix_carro_movimiento_carro ON main.carro_movimiento (empresa_id, carro_id, created_at DESC, id DESC);

-- Abonos de una tienda a lo que debe (pedidos a crédito).
CREATE TABLE IF NOT EXISTS main.abono_tienda (
    id                BIGSERIAL     PRIMARY KEY,
    empresa_id        BIGINT        NOT NULL REFERENCES main.empresa (id) ON DELETE CASCADE,
    tienda_id         BIGINT        NOT NULL,
    -- Opcional: el abono puede ir a un pedido puntual o a la deuda en general.
    pedido_tienda_id  BIGINT,
    fecha             DATE          NOT NULL,
    valor             NUMERIC(12,0) NOT NULL,
    nota              VARCHAR(200),
    usuario_id        BIGINT        REFERENCES main.usuario (id) ON DELETE SET NULL,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_abono_tienda_tienda FOREIGN KEY (tienda_id, empresa_id) REFERENCES main.tienda (id, empresa_id),
    CONSTRAINT fk_abono_tienda_pedido FOREIGN KEY (pedido_tienda_id, empresa_id)
        REFERENCES main.pedido_tienda (id, empresa_id),
    CONSTRAINT ck_abono_tienda_valor CHECK (valor > 0)
);
CREATE INDEX IF NOT EXISTS ix_abono_tienda_tienda ON main.abono_tienda (empresa_id, tienda_id, fecha DESC);
