package com.agora.catalog.correo.repository;

import com.agora.catalog.correo.model.ProveedorCorreo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public class ProveedorCorreoRepository {

    private static final RowMapper<ProveedorCorreo> MAPPER = (rs, i) -> new ProveedorCorreo(
            rs.getString("host"),
            rs.getInt("puerto"),
            rs.getString("usuario"),
            rs.getString("password_cifrada"),
            rs.getString("remitente_email"),
            rs.getString("remitente_nombre"),
            ProveedorCorreo.Seguridad.valueOf(rs.getString("seguridad")),
            rs.getBoolean("activo"),
            rs.getObject("updated_at", OffsetDateTime.class));

    private final JdbcTemplate jdbc;

    public ProveedorCorreoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ProveedorCorreo> find() {
        return jdbc.query("SELECT * FROM main.proveedor_correo WHERE id = 1", MAPPER).stream().findFirst();
    }

    public void save(ProveedorCorreo p) {
        jdbc.update("""
                INSERT INTO main.proveedor_correo
                       (id, host, puerto, usuario, password_cifrada, remitente_email, remitente_nombre, seguridad, activo)
                VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                       host = EXCLUDED.host, puerto = EXCLUDED.puerto, usuario = EXCLUDED.usuario,
                       password_cifrada = EXCLUDED.password_cifrada, remitente_email = EXCLUDED.remitente_email,
                       remitente_nombre = EXCLUDED.remitente_nombre, seguridad = EXCLUDED.seguridad,
                       activo = EXCLUDED.activo, updated_at = now()
                """, p.host(), p.puerto(), p.usuario(), p.passwordCifrada(), p.remitenteEmail(),
                p.remitenteNombre(), p.seguridad().name(), p.activo());
    }
}
