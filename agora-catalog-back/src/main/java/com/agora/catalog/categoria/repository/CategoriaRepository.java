package com.agora.catalog.categoria.repository;

import com.agora.catalog.categoria.dto.CategoriaDtos.CategoriaResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Toda consulta filtra por empresa: una categoría de otra empresa no existe para quien pregunta. */
@Repository
public class CategoriaRepository {

    private static final String SELECT = """
            SELECT c.*, (SELECT count(*) FROM main.producto p WHERE p.categoria_id = c.id) AS productos
              FROM main.categoria c
            """;

    private static final RowMapper<CategoriaResponse> MAPPER = (rs, i) -> new CategoriaResponse(
            rs.getLong("id"), rs.getString("nombre"), rs.getInt("orden"), rs.getBoolean("activa"),
            rs.getString("imagen_url"), rs.getInt("productos"), rs.getBoolean("banner_activo"),
            rs.getString("banner_url"), rs.getString("banner_titulo"), rs.getString("banner_frase"),
            rs.getString("banner_nota"));

    private final JdbcTemplate jdbc;

    public CategoriaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CategoriaResponse> findAll(long empresaId) {
        return jdbc.query(SELECT + " WHERE c.empresa_id = ? ORDER BY c.orden, lower(c.nombre)", MAPPER, empresaId);
    }

    public Optional<CategoriaResponse> findById(long empresaId, long id) {
        return jdbc.query(SELECT + " WHERE c.empresa_id = ? AND c.id = ?", MAPPER, empresaId, id).stream().findFirst();
    }

    public boolean existsByNombre(long empresaId, String nombre, Long exceptId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM main.categoria
                                WHERE empresa_id = ? AND lower(nombre) = lower(?) AND id IS DISTINCT FROM ?)
                """, Boolean.class, empresaId, nombre, exceptId));
    }

    public long insert(long empresaId, String nombre, boolean activa) {
        return jdbc.queryForObject("""
                INSERT INTO main.categoria (empresa_id, nombre, activa, orden)
                VALUES (?, ?, ?, (SELECT COALESCE(MAX(orden), 0) + 1 FROM main.categoria WHERE empresa_id = ?))
                RETURNING id
                """, Long.class, empresaId, nombre, activa, empresaId);
    }

    public int update(long empresaId, long id, String nombre, boolean activa) {
        return jdbc.update("UPDATE main.categoria SET nombre = ?, activa = ? WHERE empresa_id = ? AND id = ?",
                nombre, activa, empresaId, id);
    }

    public int delete(long empresaId, long id) {
        return jdbc.update("DELETE FROM main.categoria WHERE empresa_id = ? AND id = ?", empresaId, id);
    }

    public void setOrden(long empresaId, long id, int orden) {
        jdbc.update("UPDATE main.categoria SET orden = ? WHERE empresa_id = ? AND id = ?", orden, empresaId, id);
    }

    /** Solo una categoría por empresa tiene la franja: se apagan las demás antes de encender esta. */
    public void setFranja(long empresaId, long id, String url, String titulo, String frase, String nota) {
        jdbc.update("UPDATE main.categoria SET banner_activo = FALSE WHERE empresa_id = ? AND id <> ? AND banner_activo",
                empresaId, id);
        jdbc.update("""
                UPDATE main.categoria SET banner_activo = TRUE, banner_url = ?, banner_titulo = ?, banner_frase = ?, banner_nota = ?
                 WHERE empresa_id = ? AND id = ?
                """, url, titulo, frase, nota, empresaId, id);
    }

    public void quitarFranja(long empresaId, long id) {
        jdbc.update("UPDATE main.categoria SET banner_activo = FALSE WHERE empresa_id = ? AND id = ?", empresaId, id);
    }
}
