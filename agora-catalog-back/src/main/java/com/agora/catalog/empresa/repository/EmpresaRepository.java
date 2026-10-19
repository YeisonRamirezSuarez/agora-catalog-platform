package com.agora.catalog.empresa.repository;

import com.agora.catalog.empresa.model.Empresa;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class EmpresaRepository {

    private static final String SELECT = """
            SELECT e.id, e.nombre, e.slug, e.public_hash, e.nombre_corto, e.lema,
                   e.color_primario, e.color_secundario, e.activa,
                   (SELECT count(*) FROM main.producto p WHERE p.empresa_id = e.id) AS productos
              FROM main.empresa e
            """;

    private static final RowMapper<Empresa> MAPPER = (rs, i) -> new Empresa(
            rs.getLong("id"),
            rs.getString("nombre"),
            rs.getString("slug"),
            rs.getString("public_hash"),
            rs.getString("nombre_corto"),
            rs.getString("lema"),
            rs.getString("color_primario"),
            rs.getString("color_secundario"),
            rs.getBoolean("activa"),
            rs.getInt("productos"));

    private final JdbcTemplate jdbc;

    public EmpresaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Empresa> findAll() {
        return jdbc.query(SELECT + " ORDER BY lower(e.nombre)", MAPPER);
    }

    public Optional<Empresa> findById(long id) {
        return jdbc.query(SELECT + " WHERE e.id = ?", MAPPER, id).stream().findFirst();
    }

    public boolean existsBySlug(String slug, Long exceptId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM main.empresa WHERE slug = ? AND id IS DISTINCT FROM ?)",
                Boolean.class, slug, exceptId));
    }

    public boolean existsByPublicHash(String publicHash) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM main.empresa WHERE public_hash = ?)", Boolean.class, publicHash));
    }

    public long insert(String nombre, String slug, String publicHash) {
        return jdbc.queryForObject(
                "INSERT INTO main.empresa (nombre, slug, public_hash) VALUES (?, ?, ?) RETURNING id",
                Long.class, nombre, slug, publicHash);
    }

    public int update(long id, String nombre, String slug) {
        return jdbc.update("UPDATE main.empresa SET nombre = ?, slug = ?, updated_at = now() WHERE id = ?",
                nombre, slug, id);
    }

    public int setActiva(long id, boolean activa) {
        return jdbc.update("UPDATE main.empresa SET activa = ?, updated_at = now() WHERE id = ?", activa, id);
    }
}
