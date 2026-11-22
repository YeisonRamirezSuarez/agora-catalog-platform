package com.agora.catalog.tablero.repository;

import com.agora.catalog.common.dto.Cantidades;
import com.agora.catalog.tablero.dto.TableroDtos.PedidoDia;
import com.agora.catalog.tablero.dto.TableroDtos.ProductoVendido;
import com.agora.catalog.tablero.dto.TableroDtos.RutaDia;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/** Consultas del tablero de venta en ruta. Todas filtran por empresa y por un día [desde, hasta). */
@Repository
public class TableroRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public TableroRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static MapSqlParameterSource dia(long empresaId, OffsetDateTime desde, OffsetDateTime hasta, String zona) {
        return new MapSqlParameterSource("empresa", empresaId).addValue("desde", desde).addValue("hasta", hasta)
                .addValue("zona", zona);
    }

    /**
     * Las rutas que salieron ese día y las que venían de antes y vendieron ese día. Pedidos y plata son
     * los del día (así la suma de los carros da lo vendido del día). Un pedido cuenta en la ruta en la que
     * se hizo (la de su primer movimiento), igual que en el detalle del carro.
     */
    public List<RutaDia> rutas(long empresaId, OffsetDateTime desde, OffsetDateTime hasta, String zona) {
        return jdbc.query("""
                WITH pedido_ruta AS (
                    SELECT DISTINCT ON (m.pedido_tienda_id) m.pedido_tienda_id, m.ruta_id
                      FROM main.carro_movimiento m
                     WHERE m.empresa_id = :empresa AND m.pedido_tienda_id IS NOT NULL
                     ORDER BY m.pedido_tienda_id, m.id)
                SELECT r.id, r.carro_id, c.nombre AS carro_nombre, c.eliminado_at IS NOT NULL AS carro_eliminado,
                       r.iniciada_at, r.cerrada_at,
                       (SELECT count(*) FROM main.carro_ruta r2
                         WHERE r2.carro_id = r.carro_id
                           AND (r2.iniciada_at AT TIME ZONE :zona)::date = (r.iniciada_at AT TIME ZONE :zona)::date
                           AND (r2.iniciada_at, r2.id) <= (r.iniciada_at, r.id)) AS numero,
                       count(p.id) FILTER (WHERE p.estado = 'CONFIRMADO') AS pedidos,
                       COALESCE(sum(p.total) FILTER (WHERE p.estado = 'CONFIRMADO'), 0) AS vendido
                  FROM main.carro_ruta r
                  JOIN main.carro c ON c.id = r.carro_id AND c.empresa_id = r.empresa_id
                  LEFT JOIN pedido_ruta pr ON pr.ruta_id = r.id
                  LEFT JOIN main.pedido_tienda p ON p.id = pr.pedido_tienda_id
                                                AND p.created_at >= :desde AND p.created_at < :hasta
                 WHERE r.empresa_id = :empresa
                   AND r.iniciada_at < :hasta
                   AND (r.cerrada_at IS NULL OR r.cerrada_at >= :desde)
                 GROUP BY r.id, c.id
                HAVING r.iniciada_at >= :desde OR count(p.id) > 0
                 ORDER BY lower(c.nombre), r.iniciada_at
                """, dia(empresaId, desde, hasta, zona), (rs, i) -> new RutaDia(rs.getLong("id"), rs.getLong("carro_id"),
                rs.getString("carro_nombre"), rs.getBoolean("carro_eliminado"), rs.getInt("numero"),
                rs.getObject("iniciada_at", OffsetDateTime.class), rs.getObject("cerrada_at", OffsetDateTime.class),
                rs.getInt("pedidos"), rs.getLong("vendido")));
    }

    public List<PedidoDia> pedidos(long empresaId, OffsetDateTime desde, OffsetDateTime hasta) {
        return jdbc.query("""
                SELECT p.id, p.numero, p.tienda_nombre, c.nombre AS carro_nombre, p.total, p.estado, p.created_at
                  FROM main.pedido_tienda p JOIN main.carro c ON c.id = p.carro_id
                 WHERE p.empresa_id = :empresa AND p.created_at >= :desde AND p.created_at < :hasta
                 ORDER BY p.created_at DESC, p.id DESC
                """, dia(empresaId, desde, hasta, null), (rs, i) -> new PedidoDia(rs.getLong("id"), rs.getInt("numero"),
                rs.getString("tienda_nombre"), rs.getString("carro_nombre"), rs.getLong("total"), rs.getString("estado"),
                rs.getObject("created_at", OffsetDateTime.class)));
    }

    public List<ProductoVendido> productos(long empresaId, OffsetDateTime desde, OffsetDateTime hasta, int limite) {
        return jdbc.query("""
                SELECT max(i.producto_nombre) AS nombre, min(i.presentacion) AS presentacion, max(i.unidad_venta) AS unidad_venta,
                       sum(i.cantidad) AS cantidad, sum(i.subtotal) AS total
                  FROM main.pedido_tienda_item i
                  JOIN main.pedido_tienda p ON p.id = i.pedido_tienda_id AND p.empresa_id = i.empresa_id
                 WHERE p.empresa_id = :empresa AND p.estado = 'CONFIRMADO'
                   AND p.created_at >= :desde AND p.created_at < :hasta
                 GROUP BY COALESCE(i.producto_id::text, i.producto_nombre), lower(COALESCE(i.presentacion, ''))
                 ORDER BY sum(i.subtotal) DESC, max(i.producto_nombre)
                 LIMIT :limite
                """, dia(empresaId, desde, hasta, null).addValue("limite", limite), (rs, i) -> new ProductoVendido(
                rs.getString("nombre"), rs.getString("presentacion"), rs.getString("unidad_venta"),
                Cantidades.limpia(rs.getBigDecimal("cantidad")), rs.getLong("total")));
    }
}
