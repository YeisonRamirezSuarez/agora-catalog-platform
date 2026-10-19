package com.agora.catalog.estadistica.repository;

import com.agora.catalog.estadistica.dto.EstadisticaDtos.ProductoVisto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Contadores por día; toda consulta filtra por empresa. */
@Repository
public class EstadisticaRepository {

    /** Total de un tipo en un día. */
    public record Fila(LocalDate fecha, String tipo, int cantidad) {
    }

    private final JdbcTemplate jdbc;

    public EstadisticaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Long> empresaActiva(String publicHash) {
        return jdbc.queryForList("SELECT id FROM main.empresa WHERE public_hash = ? AND activa", Long.class, publicHash)
                .stream().findFirst();
    }

    /** Suma 1 al contador del día. */
    public void sumar(long empresaId, LocalDate fecha, String tipo) {
        jdbc.update("""
                INSERT INTO main.estadistica_diaria (empresa_id, fecha, tipo, cantidad) VALUES (?, ?, ?, 1)
                ON CONFLICT (empresa_id, fecha, tipo, (COALESCE(producto_id, 0)))
                DO UPDATE SET cantidad = main.estadistica_diaria.cantidad + 1
                """, empresaId, fecha, tipo);
    }

    /** Suma 1 a las vistas del producto, solo si es de esa empresa (un id ajeno no cuenta). */
    public void sumarProducto(long empresaId, LocalDate fecha, long productoId) {
        jdbc.update("""
                INSERT INTO main.estadistica_diaria (empresa_id, fecha, tipo, producto_id, cantidad)
                SELECT ?, ?, 'PRODUCTO', p.id, 1 FROM main.producto p WHERE p.id = ? AND p.empresa_id = ?
                ON CONFLICT (empresa_id, fecha, tipo, (COALESCE(producto_id, 0)))
                DO UPDATE SET cantidad = main.estadistica_diaria.cantidad + 1
                """, empresaId, fecha, productoId, empresaId);
    }

    public List<Fila> porDia(long empresaId, LocalDate desde, LocalDate hasta) {
        return jdbc.query("""
                SELECT fecha, tipo, SUM(cantidad) AS cantidad FROM main.estadistica_diaria
                 WHERE empresa_id = ? AND fecha BETWEEN ? AND ?
                 GROUP BY fecha, tipo
                """, (rs, i) -> new Fila(rs.getObject("fecha", LocalDate.class), rs.getString("tipo"), rs.getInt("cantidad")),
                empresaId, desde, hasta);
    }

    public List<ProductoVisto> masVistos(long empresaId, LocalDate desde, LocalDate hasta, int limite) {
        return jdbc.query("""
                SELECT p.id, p.nombre, p.imagen_thumb_url, SUM(e.cantidad) AS vistas
                  FROM main.estadistica_diaria e JOIN main.producto p ON p.id = e.producto_id AND p.empresa_id = e.empresa_id
                 WHERE e.empresa_id = ? AND e.tipo = 'PRODUCTO' AND e.fecha BETWEEN ? AND ?
                 GROUP BY p.id, p.nombre, p.imagen_thumb_url
                 ORDER BY vistas DESC, p.nombre
                 LIMIT ?
                """, (rs, i) -> new ProductoVisto(rs.getLong("id"), rs.getString("nombre"), rs.getString("imagen_thumb_url"),
                rs.getInt("vistas")), empresaId, desde, hasta, limite);
    }
}
