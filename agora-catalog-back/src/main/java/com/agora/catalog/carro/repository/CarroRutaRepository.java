package com.agora.catalog.carro.repository;

import com.agora.catalog.carro.dto.CarroDtos.PedidoRuta;
import com.agora.catalog.carro.dto.CarroDtos.RutaItem;
import com.agora.catalog.carro.dto.CarroDtos.RutaResponse;
import com.agora.catalog.common.dto.Cantidades;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Rutas (salidas) de un carro. Hay a lo sumo una en curso por carro ({@code cerrada_at IS NULL}) y los
 * movimientos del carro caen en ella ({@link CarroStockRepository#movimiento}).
 */
@Repository
public class CarroRutaRepository {

    private record Cabecera(long id, int numero, OffsetDateTime iniciada, OffsetDateTime cerrada) {
    }

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;

    public CarroRutaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.named = new NamedParameterJdbcTemplate(jdbc);
    }

    public Optional<Long> enCurso(long empresaId, long carroId) {
        return jdbc.query("SELECT id FROM main.carro_ruta WHERE empresa_id = ? AND carro_id = ? AND cerrada_at IS NULL",
                (rs, i) -> rs.getLong("id"), empresaId, carroId).stream().findFirst();
    }

    public long abrir(long empresaId, long carroId, long usuarioId) {
        return jdbc.queryForObject("INSERT INTO main.carro_ruta (empresa_id, carro_id, usuario_id) VALUES (?, ?, ?) RETURNING id",
                Long.class, empresaId, carroId, usuarioId);
    }

    public void cerrar(long empresaId, long rutaId) {
        jdbc.update("UPDATE main.carro_ruta SET cerrada_at = now() WHERE empresa_id = ? AND id = ?", empresaId, rutaId);
    }

    /** Si en la ruta pasó algo más que traer lo de la anterior (cargar, vender, quitar). */
    public boolean tuvoActividad(long empresaId, long rutaId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM main.carro_movimiento WHERE empresa_id = ? AND ruta_id = ? AND tipo <> 'INICIAL')
                """, Boolean.class, empresaId, rutaId));
    }

    /** Borra una ruta en la que no pasó nada (lo que traía de la anterior no movió el stock). */
    public void borrarVacia(long empresaId, long rutaId) {
        jdbc.update("DELETE FROM main.carro_movimiento WHERE empresa_id = ? AND ruta_id = ? AND tipo = 'INICIAL'", empresaId, rutaId);
        jdbc.update("DELETE FROM main.carro_ruta WHERE empresa_id = ? AND id = ?", empresaId, rutaId);
    }

    public int contar(long empresaId, long carroId) {
        return jdbc.queryForObject("SELECT count(*) FROM main.carro_ruta WHERE empresa_id = ? AND carro_id = ?",
                Integer.class, empresaId, carroId);
    }

    /** Las rutas, de la más reciente a la más vieja, numeradas dentro de su día y con sus productos. */
    public List<RutaResponse> rutas(long empresaId, long carroId, int page, int size) {
        List<Cabecera> cabeceras = jdbc.query("""
                SELECT r.id, r.iniciada_at, r.cerrada_at,
                       row_number() OVER (PARTITION BY (r.iniciada_at AT TIME ZONE ?)::date
                                          ORDER BY r.iniciada_at, r.id) AS numero
                  FROM main.carro_ruta r
                 WHERE r.empresa_id = ? AND r.carro_id = ?
                 ORDER BY r.iniciada_at DESC, r.id DESC LIMIT ? OFFSET ?
                """, (rs, i) -> new Cabecera(rs.getLong("id"), rs.getInt("numero"),
                        rs.getObject("iniciada_at", OffsetDateTime.class), rs.getObject("cerrada_at", OffsetDateTime.class)),
                ZoneId.systemDefault().getId(), empresaId, carroId, size, (long) page * size);
        if (cabeceras.isEmpty()) return List.of();

        Map<Long, List<RutaItem>> items = new HashMap<>();
        named.query("""
                SELECT m.ruta_id, m.producto_id, max(COALESCE(p.nombre, m.producto_nombre)) AS nombre,
                       min(m.presentacion) AS presentacion, COALESCE(max(p.unidad_venta), 'UNIDAD') AS unidad_venta,
                       COALESCE(sum(m.cantidad) FILTER (WHERE m.tipo = 'INICIAL'), 0) AS traia,
                       COALESCE(sum(m.cantidad) FILTER (WHERE m.tipo = 'CARGA' OR (m.tipo = 'AJUSTE' AND m.cantidad > 0)), 0) AS cargado,
                       -COALESCE(sum(m.cantidad) FILTER (WHERE m.tipo IN ('VENTA', 'ANULACION')), 0) AS vendido,
                       -COALESCE(sum(m.cantidad) FILTER (WHERE m.tipo = 'DESCARGUE' OR (m.tipo = 'AJUSTE' AND m.cantidad < 0)), 0) AS quitado,
                       sum(m.cantidad) AS queda
                  FROM main.carro_movimiento m LEFT JOIN main.producto p ON p.id = m.producto_id
                 WHERE m.empresa_id = :empresa AND m.ruta_id IN (:rutas)
                 GROUP BY m.ruta_id, m.producto_id, lower(COALESCE(m.presentacion, ''))
                 ORDER BY min(m.id)
                """, new MapSqlParameterSource("empresa", empresaId)
                        .addValue("rutas", cabeceras.stream().map(Cabecera::id).toList()),
                rs -> {
                    items.computeIfAbsent(rs.getLong("ruta_id"), k -> new ArrayList<>()).add(item(rs));
                });

        // Un pedido es de la ruta en la que se hizo (su primer movimiento): corregirlo después no lo cambia de ruta.
        Map<Long, List<PedidoRuta>> pedidos = new HashMap<>();
        named.query("""
                SELECT x.ruta_id, p.id, p.numero, p.tienda_nombre AS tienda, p.total, p.estado, p.created_at
                  FROM (SELECT DISTINCT ON (m.pedido_tienda_id) m.pedido_tienda_id, m.ruta_id
                          FROM main.carro_movimiento m
                         WHERE m.empresa_id = :empresa AND m.carro_id = :carro AND m.pedido_tienda_id IS NOT NULL
                         ORDER BY m.pedido_tienda_id, m.id) x
                  JOIN main.pedido_tienda p ON p.id = x.pedido_tienda_id AND p.empresa_id = :empresa
                 WHERE x.ruta_id IN (:rutas)
                 ORDER BY p.created_at, p.id
                """, new MapSqlParameterSource("empresa", empresaId).addValue("carro", carroId)
                        .addValue("rutas", cabeceras.stream().map(Cabecera::id).toList()),
                rs -> {
                    pedidos.computeIfAbsent(rs.getLong("ruta_id"), k -> new ArrayList<>()).add(new PedidoRuta(
                            rs.getLong("id"), rs.getInt("numero"), rs.getString("tienda"), rs.getLong("total"),
                            rs.getString("estado"), rs.getObject("created_at", OffsetDateTime.class)));
                });

        return cabeceras.stream().map(c -> new RutaResponse(c.id(), c.numero(), c.iniciada(), c.cerrada(),
                items.getOrDefault(c.id(), List.of()), pedidos.getOrDefault(c.id(), List.of()))).toList();
    }

    private static RutaItem item(ResultSet rs) throws SQLException {
        return new RutaItem(rs.getObject("producto_id", Long.class), rs.getString("nombre"), rs.getString("presentacion"),
                rs.getString("unidad_venta"), cant(rs, "traia"), cant(rs, "cargado"), cant(rs, "vendido"),
                cant(rs, "quitado"), cant(rs, "queda"));
    }

    private static BigDecimal cant(ResultSet rs, String columna) throws SQLException {
        return Cantidades.limpia(rs.getBigDecimal(columna));
    }
}
