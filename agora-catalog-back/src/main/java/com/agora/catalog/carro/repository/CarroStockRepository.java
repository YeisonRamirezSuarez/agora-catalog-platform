package com.agora.catalog.carro.repository;

import com.agora.catalog.carro.dto.CarroDtos.MovimientoResponse;
import com.agora.catalog.carro.dto.CarroDtos.StockItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.agora.catalog.common.dto.Cantidades;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Stock de cada carro y sus movimientos. El servicio bloquea el carro antes de llamar a los métodos
 * que escriben ({@link CarroRepository#lock}), así que acá no hay carreras entre leer y escribir.
 */
@Repository
public class CarroStockRepository {

    /** Lo que hace falta de un producto para cargarlo o venderlo: nombre, unidad, precio y presentaciones. */
    public record ProductoCarga(long id, String nombre, String presentacion, String unidadVenta, long precio, List<Otra> otras) {
    }

    /** Otra presentación del producto con su precio de catálogo. */
    public record Otra(String nombre, long precio) {
    }

    /** Una fila de stock tal como está en la base (para descargar todo). */
    public record Fila(long productoId, String productoNombre, String presentacion, BigDecimal cantidad) {
    }

    private final JdbcTemplate jdbc;

    public CarroStockRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ProductoCarga> producto(long empresaId, long productoId) {
        return jdbc.query("SELECT id, nombre, presentacion, unidad_venta, precio FROM main.producto WHERE empresa_id = ? AND id = ?",
                (rs, i) -> new ProductoCarga(rs.getLong("id"), rs.getString("nombre"), rs.getString("presentacion"),
                        rs.getString("unidad_venta"), rs.getLong("precio"), List.of()), empresaId, productoId).stream().findFirst()
                .map(p -> new ProductoCarga(p.id(), p.nombre(), p.presentacion(), p.unidadVenta(), p.precio(), jdbc.query(
                        "SELECT nombre, precio FROM main.producto_presentacion WHERE empresa_id = ? AND producto_id = ? ORDER BY orden",
                        (rs, i) -> new Otra(rs.getString("nombre"), rs.getLong("precio")), empresaId, productoId)));
    }

    public List<StockItem> stock(long empresaId, long carroId) {
        return jdbc.query("""
                SELECT s.producto_id, p.codigo, p.nombre, s.presentacion, p.unidad_venta, s.cantidad, p.imagen_thumb_url,
                       %s AS precio
                  FROM main.carro_stock s JOIN main.producto p ON p.id = s.producto_id
                  JOIN main.categoria c ON c.id = p.categoria_id
                 WHERE s.empresa_id = ? AND s.carro_id = ?
                 ORDER BY c.orden, p.orden, lower(p.nombre), s.presentacion NULLS FIRST
                """.formatted(CarroRepository.PRECIO_STOCK), (rs, i) -> new StockItem(
                rs.getLong("producto_id"), rs.getString("codigo"), rs.getString("nombre"), rs.getString("presentacion"),
                rs.getString("unidad_venta"), Cantidades.limpia(rs.getBigDecimal("cantidad")), rs.getLong("precio"),
                rs.getString("imagen_thumb_url")), empresaId, carroId);
    }

    public List<Fila> filas(long empresaId, long carroId) {
        return jdbc.query("""
                SELECT s.producto_id, p.nombre, s.presentacion, s.cantidad
                  FROM main.carro_stock s JOIN main.producto p ON p.id = s.producto_id
                 WHERE s.empresa_id = ? AND s.carro_id = ?
                """, (rs, i) -> new Fila(rs.getLong("producto_id"), rs.getString("nombre"), rs.getString("presentacion"),
                rs.getBigDecimal("cantidad")), empresaId, carroId);
    }

    public BigDecimal cantidad(long empresaId, long carroId, long productoId, String presentacion) {
        return jdbc.query("""
                SELECT cantidad FROM main.carro_stock
                 WHERE empresa_id = ? AND carro_id = ? AND producto_id = ?
                   AND lower(COALESCE(presentacion, '')) = lower(COALESCE(?, ''))
                """, (rs, i) -> rs.getBigDecimal("cantidad"), empresaId, carroId, productoId, presentacion)
                .stream().findFirst().orElse(BigDecimal.ZERO);
    }

    /** Deja la cantidad del producto en el carro; en cero, borra la fila. */
    public void setCantidad(long empresaId, long carroId, long productoId, String presentacion, BigDecimal cantidad) {
        if (cantidad.signum() == 0) {
            jdbc.update("""
                    DELETE FROM main.carro_stock
                     WHERE empresa_id = ? AND carro_id = ? AND producto_id = ?
                       AND lower(COALESCE(presentacion, '')) = lower(COALESCE(?, ''))
                    """, empresaId, carroId, productoId, presentacion);
            return;
        }
        jdbc.update("""
                INSERT INTO main.carro_stock (empresa_id, carro_id, producto_id, presentacion, cantidad)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (carro_id, producto_id, lower(COALESCE(presentacion, '')))
                DO UPDATE SET cantidad = EXCLUDED.cantidad, updated_at = now()
                """, empresaId, carroId, productoId, presentacion, cantidad);
    }

    public void movimiento(long empresaId, long carroId, long usuarioId, long productoId, String productoNombre,
                           String presentacion, String tipo, BigDecimal cantidad, BigDecimal saldo, String motivo) {
        movimiento(empresaId, carroId, usuarioId, productoId, productoNombre, presentacion, tipo, cantidad, saldo, motivo, null);
    }

    /** Con {@code pedidoId} cuando el movimiento sale de un pedido a una tienda (venta o anulación). */
    public void movimiento(long empresaId, long carroId, long usuarioId, long productoId, String productoNombre,
                           String presentacion, String tipo, BigDecimal cantidad, BigDecimal saldo, String motivo,
                           Long pedidoId) {
        // Todo movimiento cae en la ruta en curso; si el carro no tiene, se abre una (el carro está bloqueado).
        jdbc.update("""
                INSERT INTO main.carro_ruta (empresa_id, carro_id, usuario_id)
                SELECT ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM main.carro_ruta WHERE carro_id = ? AND cerrada_at IS NULL)
                """, empresaId, carroId, usuarioId, carroId);
        jdbc.update("""
                INSERT INTO main.carro_movimiento (empresa_id, carro_id, producto_id, producto_nombre, presentacion,
                                                   tipo, cantidad, saldo, motivo, usuario_id, pedido_tienda_id, ruta_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                        (SELECT id FROM main.carro_ruta WHERE empresa_id = ? AND carro_id = ? AND cerrada_at IS NULL))
                """, empresaId, carroId, productoId, productoNombre, presentacion, tipo, cantidad, saldo, motivo, usuarioId,
                pedidoId, empresaId, carroId);
    }

    public List<MovimientoResponse> movimientos(long empresaId, long carroId, int page, int size) {
        return jdbc.query("""
                SELECT m.*, u.nombre AS usuario_nombre
                  FROM main.carro_movimiento m LEFT JOIN main.usuario u ON u.id = m.usuario_id
                 WHERE m.empresa_id = ? AND m.carro_id = ?
                 ORDER BY m.created_at DESC, m.id DESC LIMIT ? OFFSET ?
                """, (rs, i) -> new MovimientoResponse(
                rs.getLong("id"), rs.getString("tipo"), (Long) rs.getObject("producto_id", Long.class),
                rs.getString("producto_nombre"), rs.getString("presentacion"),
                Cantidades.limpia(rs.getBigDecimal("cantidad")), Cantidades.limpia(rs.getBigDecimal("saldo")),
                rs.getString("motivo"), rs.getObject("pedido_tienda_id", Long.class), rs.getString("usuario_nombre"),
                rs.getObject("created_at", OffsetDateTime.class)), empresaId, carroId, size, (long) page * size);
    }

    public int contarMovimientos(long empresaId, long carroId) {
        return jdbc.queryForObject("SELECT count(*) FROM main.carro_movimiento WHERE empresa_id = ? AND carro_id = ?",
                Integer.class, empresaId, carroId);
    }
}
