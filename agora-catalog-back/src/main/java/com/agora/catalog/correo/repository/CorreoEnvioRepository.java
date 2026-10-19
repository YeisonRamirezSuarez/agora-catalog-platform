package com.agora.catalog.correo.repository;

import com.agora.catalog.correo.dto.response.CorreoEnvioResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class CorreoEnvioRepository {

    private static final RowMapper<CorreoEnvioResponse> MAPPER = (rs, i) -> new CorreoEnvioResponse(
            rs.getLong("id"),
            rs.getObject("empresa_id", Long.class),
            rs.getString("destinatario"),
            rs.getString("asunto"),
            rs.getString("tipo"),
            rs.getBoolean("enviado"),
            rs.getString("error"),
            rs.getObject("created_at", OffsetDateTime.class));

    private final JdbcTemplate jdbc;

    public CorreoEnvioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(Long empresaId, String destinatario, String asunto, String tipo, boolean enviado, String error) {
        jdbc.update("""
                INSERT INTO main.correo_envio (empresa_id, destinatario, asunto, tipo, enviado, error)
                VALUES (?, ?, ?, ?, ?, ?)
                """, empresaId, destinatario, asunto, tipo, enviado,
                error == null ? null : error.substring(0, Math.min(error.length(), 1000)));
    }

    public List<CorreoEnvioResponse> findRecientes(int limite) {
        return jdbc.query("SELECT * FROM main.correo_envio ORDER BY created_at DESC, id DESC LIMIT ?", MAPPER, limite);
    }
}
