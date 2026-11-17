package com.agora.catalog.tienda.repository;

import com.agora.catalog.tienda.dto.TiendaDtos.TiendaResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Toda consulta filtra por empresa: una tienda de otra empresa no existe para quien pregunta. */
@Repository
public class TiendaRepository {

    /** Datos que se guardan, ya limpios (sin espacios de más, vacíos como null). */
    public record Datos(String nombre, String encargado, String telefono, String direccion, String barrio,
                        String notas, boolean activa) {
    }

    private static final String SELECT = """
            SELECT t.*, (SELECT count(*) FROM main.pedido_tienda p WHERE p.tienda_id = t.id) AS pedidos
              FROM main.tienda t
            """;

    private static final RowMapper<TiendaResponse> MAPPER = (rs, i) -> new TiendaResponse(
            rs.getLong("id"), rs.getString("nombre"), rs.getString("encargado"), rs.getString("telefono"),
            rs.getString("direccion"), rs.getString("barrio"), rs.getString("notas"), rs.getBoolean("activa"),
            rs.getInt("pedidos"));

    private final JdbcTemplate jdbc;

    public TiendaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<TiendaResponse> findById(long empresaId, long id) {
        return jdbc.query(SELECT + " WHERE t.empresa_id = ? AND t.id = ?", MAPPER, empresaId, id).stream().findFirst();
    }

    public List<TiendaResponse> page(long empresaId, String q, Boolean activa, int page, int size) {
        List<Object> args = new ArrayList<>();
        String where = where(empresaId, q, activa, args);
        args.add(size);
        args.add((long) page * size);
        return jdbc.query(SELECT + where + " ORDER BY t.activa DESC, lower(t.nombre), t.id LIMIT ? OFFSET ?",
                MAPPER, args.toArray());
    }

    public int count(long empresaId, String q, Boolean activa) {
        List<Object> args = new ArrayList<>();
        String where = where(empresaId, q, activa, args);
        return jdbc.queryForObject("SELECT count(*) FROM main.tienda t" + where, Integer.class, args.toArray());
    }

    private static String where(long empresaId, String q, Boolean activa, List<Object> args) {
        StringBuilder w = new StringBuilder(" WHERE t.empresa_id = ?");
        args.add(empresaId);
        if (q != null && !q.isBlank()) {
            // Sin tildes ni mayúsculas, en nombre, encargado, barrio y dirección.
            w.append(" AND translate(lower(concat_ws(' ', t.nombre, t.encargado, t.barrio, t.direccion)), 'áéíóúüñ', 'aeiouun')"
                    + " LIKE translate(lower(?), 'áéíóúüñ', 'aeiouun')");
            args.add("%" + q.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
        }
        if (activa != null) {
            w.append(" AND t.activa = ?");
            args.add(activa);
        }
        return w.toString();
    }

    public boolean existsByNombre(long empresaId, String nombre, Long exceptId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM main.tienda
                                WHERE empresa_id = ? AND lower(nombre) = lower(?) AND id IS DISTINCT FROM ?)
                """, Boolean.class, empresaId, nombre, exceptId));
    }

    /** La tienda con ese nombre, sin importar mayúsculas ni espacios de más. */
    public Optional<Long> findIdByNombre(long empresaId, String nombre) {
        return jdbc.queryForList("SELECT id FROM main.tienda WHERE empresa_id = ? AND lower(nombre) = lower(?)",
                Long.class, empresaId, nombre).stream().findFirst();
    }

    public long insert(long empresaId, Datos d) {
        return jdbc.queryForObject("""
                INSERT INTO main.tienda (empresa_id, nombre, encargado, telefono, direccion, barrio, notas, activa)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, empresaId, d.nombre(), d.encargado(), d.telefono(), d.direccion(), d.barrio(),
                d.notas(), d.activa());
    }

    public int update(long empresaId, long id, Datos d) {
        return jdbc.update("""
                UPDATE main.tienda SET nombre = ?, encargado = ?, telefono = ?, direccion = ?, barrio = ?, notas = ?,
                       activa = ?, updated_at = now()
                 WHERE empresa_id = ? AND id = ?
                """, d.nombre(), d.encargado(), d.telefono(), d.direccion(), d.barrio(), d.notas(), d.activa(),
                empresaId, id);
    }

    public int setActiva(long empresaId, long id, boolean activa) {
        return jdbc.update("UPDATE main.tienda SET activa = ?, updated_at = now() WHERE empresa_id = ? AND id = ?",
                activa, empresaId, id);
    }

    public int delete(long empresaId, long id) {
        return jdbc.update("DELETE FROM main.tienda WHERE empresa_id = ? AND id = ?", empresaId, id);
    }
}
