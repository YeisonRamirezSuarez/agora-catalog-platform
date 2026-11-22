package com.agora.catalog.pedidotienda.repository;

import com.agora.catalog.pedidotienda.dto.PedidoTiendaDtos.ItemResponse;
import com.agora.catalog.pedidotienda.dto.PedidoTiendaDtos.PedidoResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.agora.catalog.common.dto.Cantidades;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** Pedidos a tiendas. Toda consulta filtra por empresa. */
@Repository
public class PedidoTiendaRepository {

    /** Clave del candado del consecutivo (pg_advisory_xact_lock(clave, empresa)). */
    private static final int CANDADO_NUMERO = 7_001;

    private final JdbcTemplate jdbc;

    public PedidoTiendaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * El siguiente número de pedido de la empresa. El candado dura hasta el final de la transacción:
     * dos pedidos al mismo tiempo (de dos carros) no sacan el mismo número.
     */
    public int siguienteNumero(long empresaId) {
        jdbc.queryForList("SELECT pg_advisory_xact_lock(?, ?)", CANDADO_NUMERO, (int) empresaId);
        return jdbc.queryForObject("SELECT COALESCE(MAX(numero), 0) + 1 FROM main.pedido_tienda WHERE empresa_id = ?",
                Integer.class, empresaId);
    }

    public long insert(long empresaId, int numero, long carroId, String tiendaNombre, long total, String notas, long usuarioId) {
        return jdbc.queryForObject("""
                INSERT INTO main.pedido_tienda (empresa_id, numero, carro_id, tienda_nombre, forma_pago, total, saldo_pendiente,
                                                estado, notas, usuario_id)
                VALUES (?, ?, ?, ?, 'CONTADO', ?, 0, 'CONFIRMADO', ?, ?) RETURNING id
                """, Long.class, empresaId, numero, carroId, tiendaNombre, total, notas, usuarioId);
    }

    /** Cómo se escribió ese nombre de tienda la última vez (sin importar mayúsculas ni tildes), si ya se usó. */
    public Optional<String> nombreUsado(long empresaId, String nombre) {
        return jdbc.queryForList("""
                SELECT tienda_nombre FROM main.pedido_tienda
                 WHERE empresa_id = ? AND translate(lower(tienda_nombre), 'áéíóúüñ', 'aeiouun') = translate(lower(?), 'áéíóúüñ', 'aeiouun')
                 ORDER BY created_at DESC LIMIT 1
                """, String.class, empresaId, nombre).stream().findFirst();
    }

    /** Nombres de tienda ya usados en pedidos, los más recientes primero, para sugerirlos al escribir. */
    public List<String> nombresTiendas(long empresaId, String q, int limite) {
        return jdbc.queryForList("""
                SELECT tienda_nombre FROM (
                    SELECT DISTINCT ON (lower(tienda_nombre)) tienda_nombre, created_at
                      FROM main.pedido_tienda
                     WHERE empresa_id = ?
                       AND translate(lower(tienda_nombre), 'áéíóúüñ', 'aeiouun') LIKE translate(lower(?), 'áéíóúüñ', 'aeiouun')
                     ORDER BY lower(tienda_nombre), created_at DESC) x
                 ORDER BY created_at DESC LIMIT ?
                """, String.class, empresaId, "%" + q.replace("\\", "").replace("%", "").replace("_", "") + "%", limite);
    }

    public void insertItem(long empresaId, long pedidoId, long productoId, String nombre, String presentacion,
                           String unidad, BigDecimal cantidad, long sugerido, long unitario, long subtotal) {
        jdbc.update("""
                INSERT INTO main.pedido_tienda_item (empresa_id, pedido_tienda_id, producto_id, producto_nombre, presentacion,
                                                     unidad_venta, cantidad, precio_sugerido, precio_unitario, subtotal)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, empresaId, pedidoId, productoId, nombre, presentacion, unidad, cantidad, sugerido, unitario, subtotal);
    }

    public Optional<PedidoResponse> findById(long empresaId, long id) {
        List<ItemResponse> items = jdbc.query("""
                SELECT * FROM main.pedido_tienda_item WHERE empresa_id = ? AND pedido_tienda_id = ? ORDER BY id
                """, (rs, i) -> new ItemResponse(rs.getObject("producto_id", Long.class), rs.getString("producto_nombre"),
                rs.getString("presentacion"), rs.getString("unidad_venta"), Cantidades.limpia(rs.getBigDecimal("cantidad")),
                rs.getLong("precio_sugerido"), rs.getLong("precio_unitario"), rs.getLong("subtotal")), empresaId, id);
        return jdbc.query("""
                SELECT p.*, c.nombre AS carro_nombre, c.eliminado_at IS NOT NULL AS carro_eliminado, u.nombre AS usuario_nombre
                  FROM main.pedido_tienda p
                  JOIN main.carro c ON c.id = p.carro_id
                  LEFT JOIN main.usuario u ON u.id = p.usuario_id
                 WHERE p.empresa_id = ? AND p.id = ?
                """, (rs, i) -> new PedidoResponse(rs.getLong("id"), rs.getInt("numero"), rs.getLong("carro_id"),
                rs.getString("carro_nombre"), rs.getBoolean("carro_eliminado"), rs.getString("tienda_nombre"), rs.getString("forma_pago"),
                rs.getLong("total"), rs.getString("estado"), rs.getString("notas"), rs.getString("usuario_nombre"),
                rs.getObject("created_at", OffsetDateTime.class), rs.getObject("editado_at", OffsetDateTime.class), items),
                empresaId, id).stream().findFirst();
    }

    /** Lo que hace falta para editar un pedido, con la fila bloqueada hasta el final de la transacción. */
    public record Cabecera(long id, int numero, long carroId, String estado) {
    }

    public Optional<Long> carroDe(long empresaId, long id) {
        return jdbc.queryForList("SELECT carro_id FROM main.pedido_tienda WHERE empresa_id = ? AND id = ?",
                Long.class, empresaId, id).stream().findFirst();
    }

    public Optional<Cabecera> lock(long empresaId, long id) {
        return jdbc.query("SELECT id, numero, carro_id, estado FROM main.pedido_tienda WHERE empresa_id = ? AND id = ? FOR UPDATE",
                (rs, i) -> new Cabecera(rs.getLong("id"), rs.getInt("numero"), rs.getLong("carro_id"), rs.getString("estado")),
                empresaId, id).stream().findFirst();
    }

    /** Una línea tal como quedó guardada: producto (null si se borró del catálogo), presentación y cantidad. */
    public record LineaGuardada(Long productoId, String presentacion, BigDecimal cantidad, long subtotal) {
    }

    public List<LineaGuardada> lineas(long empresaId, long pedidoId) {
        return jdbc.query("""
                SELECT producto_id, presentacion, cantidad, subtotal FROM main.pedido_tienda_item
                 WHERE empresa_id = ? AND pedido_tienda_id = ? ORDER BY id
                """, (rs, i) -> new LineaGuardada(rs.getObject("producto_id", Long.class), rs.getString("presentacion"),
                rs.getBigDecimal("cantidad"), rs.getLong("subtotal")), empresaId, pedidoId);
    }

    /** Borra las líneas que se reemplazan; las de productos que ya no existen en el catálogo se conservan. */
    public void borrarLineasConProducto(long empresaId, long pedidoId) {
        jdbc.update("DELETE FROM main.pedido_tienda_item WHERE empresa_id = ? AND pedido_tienda_id = ? AND producto_id IS NOT NULL",
                empresaId, pedidoId);
    }

    public void actualizar(long empresaId, long pedidoId, String tiendaNombre, long total, String notas) {
        jdbc.update("""
                UPDATE main.pedido_tienda SET tienda_nombre = ?, total = ?, notas = ?, editado_at = now()
                 WHERE empresa_id = ? AND id = ?
                """, tiendaNombre, total, notas, empresaId, pedidoId);
    }
}
