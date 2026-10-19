package com.agora.catalog.usuario.repository;

import com.agora.catalog.usuario.model.Rol;
import com.agora.catalog.usuario.model.Usuario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UsuarioRepository {

    private static final String SELECT = """
            SELECT u.id, u.empresa_id, e.nombre AS empresa_nombre, COALESCE(e.activa, TRUE) AS empresa_activa,
                   u.nombre, u.email, u.password_hash, u.rol, u.activo, u.debe_cambiar_password
              FROM main.usuario u
              LEFT JOIN main.empresa e ON e.id = u.empresa_id
            """;

    private static final RowMapper<Usuario> MAPPER = (rs, i) -> new Usuario(
            rs.getLong("id"),
            rs.getObject("empresa_id", Long.class),
            rs.getString("empresa_nombre"),
            rs.getBoolean("empresa_activa"),
            rs.getString("nombre"),
            rs.getString("email"),
            rs.getString("password_hash"),
            Rol.valueOf(rs.getString("rol")),
            rs.getBoolean("activo"),
            rs.getBoolean("debe_cambiar_password"));

    private final JdbcTemplate jdbc;

    public UsuarioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Usuario> findByEmail(String email) {
        return jdbc.query(SELECT + " WHERE lower(u.email) = lower(?)", MAPPER, email.trim()).stream().findFirst();
    }

    public Optional<Usuario> findById(long id) {
        return jdbc.query(SELECT + " WHERE u.id = ?", MAPPER, id).stream().findFirst();
    }

    public List<Usuario> findByEmpresaIds(List<Long> empresaIds) {
        if (empresaIds.isEmpty()) return List.of();
        String marks = String.join(",", empresaIds.stream().map(id -> "?").toList());
        return jdbc.query(SELECT + " WHERE u.empresa_id IN (" + marks + ") ORDER BY u.id", MAPPER, empresaIds.toArray());
    }

    public boolean existsByEmail(String email) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM main.usuario WHERE lower(email) = lower(?))", Boolean.class, email.trim()));
    }

    public long insert(Long empresaId, String nombre, String email, String passwordHash, Rol rol, boolean debeCambiarPassword) {
        return jdbc.queryForObject("""
                INSERT INTO main.usuario (empresa_id, nombre, email, password_hash, rol, debe_cambiar_password)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, empresaId, nombre.trim(), email.trim(), passwordHash, rol.name(), debeCambiarPassword);
    }

    public void updatePassword(long id, String passwordHash) {
        jdbc.update("UPDATE main.usuario SET password_hash = ?, debe_cambiar_password = FALSE WHERE id = ?",
                passwordHash, id);
    }

    /** Contraseña temporal puesta por el super admin: se pide cambiarla al entrar. */
    public void setPasswordTemporal(long id, String passwordHash) {
        jdbc.update("UPDATE main.usuario SET password_hash = ?, debe_cambiar_password = TRUE WHERE id = ?",
                passwordHash, id);
    }

    public int setActivo(long id, boolean activo) {
        return jdbc.update("UPDATE main.usuario SET activo = ? WHERE id = ?", activo, id);
    }
}
