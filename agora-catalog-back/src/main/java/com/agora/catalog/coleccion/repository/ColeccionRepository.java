package com.agora.catalog.coleccion.repository;

import com.agora.catalog.coleccion.dto.ColeccionDtos.ColeccionResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Colecciones de una empresa con los ids de sus productos, en orden. */
@Repository
public class ColeccionRepository {

    private final JdbcTemplate jdbc;

    public ColeccionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ColeccionResponse> findAll(long empresaId) {
        return query(" WHERE k.empresa_id = ?", empresaId);
    }

    public Optional<ColeccionResponse> findById(long empresaId, long id) {
        return query(" WHERE k.empresa_id = ? AND k.id = ?", empresaId, id).stream().findFirst();
    }

    private List<ColeccionResponse> query(String where, Object... args) {
        record Fila(long id, String nombre, String descripcion, String imagenUrl, boolean visible, int orden, Long producto) {
        }
        List<Fila> filas = jdbc.query("""
                SELECT k.id, k.nombre, k.descripcion, k.imagen_url, k.visible, k.orden, cp.producto_id
                  FROM main.coleccion k LEFT JOIN main.coleccion_producto cp ON cp.coleccion_id = k.id
                """ + where + " ORDER BY k.orden, k.id, cp.orden", (rs, i) -> new Fila(rs.getLong("id"),
                rs.getString("nombre"), rs.getString("descripcion"), rs.getString("imagen_url"), rs.getBoolean("visible"),
                rs.getInt("orden"), rs.getObject("producto_id", Long.class)), args);
        Map<Long, Fila> cabeceras = new LinkedHashMap<>();
        Map<Long, List<Long>> productos = new LinkedHashMap<>();
        for (Fila f : filas) {
            cabeceras.putIfAbsent(f.id(), f);
            List<Long> ids = productos.computeIfAbsent(f.id(), k -> new ArrayList<>());
            if (f.producto() != null) ids.add(f.producto());
        }
        return cabeceras.values().stream().map(f -> new ColeccionResponse(f.id(), f.nombre(), f.descripcion(),
                f.imagenUrl(), f.visible(), f.orden(), productos.get(f.id()))).toList();
    }

    public int count(long empresaId) {
        return jdbc.queryForObject("SELECT count(*) FROM main.coleccion WHERE empresa_id = ?", Integer.class, empresaId);
    }

    /** Cuántos de estos ids son productos de la empresa (para no aceptar productos ajenos). */
    public int productosPropios(long empresaId, List<Long> ids) {
        if (ids.isEmpty()) return 0;
        return jdbc.queryForObject("SELECT count(*) FROM main.producto WHERE empresa_id = ? AND id = ANY (?)",
                Integer.class, empresaId, ids.toArray(Long[]::new));
    }

    public long insert(long empresaId, String nombre, String descripcion, boolean visible) {
        return jdbc.queryForObject("""
                INSERT INTO main.coleccion (empresa_id, nombre, descripcion, visible, orden)
                VALUES (?, ?, ?, ?, (SELECT COALESCE(MAX(orden), 0) + 1 FROM main.coleccion WHERE empresa_id = ?)) RETURNING id
                """, Long.class, empresaId, nombre, descripcion, visible, empresaId);
    }

    public void update(long empresaId, long id, String nombre, String descripcion, boolean visible) {
        jdbc.update("UPDATE main.coleccion SET nombre = ?, descripcion = ?, visible = ? WHERE empresa_id = ? AND id = ?",
                nombre, descripcion, visible, empresaId, id);
    }

    public void setProductos(long id, List<Long> productos) {
        jdbc.update("DELETE FROM main.coleccion_producto WHERE coleccion_id = ?", id);
        for (int i = 0; i < productos.size(); i++) {
            jdbc.update("INSERT INTO main.coleccion_producto (coleccion_id, producto_id, orden) VALUES (?, ?, ?)",
                    id, productos.get(i), i + 1);
        }
    }

    public void setImagen(long empresaId, long id, String url) {
        jdbc.update("UPDATE main.coleccion SET imagen_url = ? WHERE empresa_id = ? AND id = ?", url, empresaId, id);
    }

    public void setOrden(long empresaId, long id, int orden) {
        jdbc.update("UPDATE main.coleccion SET orden = ? WHERE empresa_id = ? AND id = ?", orden, empresaId, id);
    }

    public void delete(long empresaId, long id) {
        jdbc.update("DELETE FROM main.coleccion WHERE empresa_id = ? AND id = ?", empresaId, id);
    }
}
