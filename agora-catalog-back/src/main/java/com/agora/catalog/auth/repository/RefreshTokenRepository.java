package com.agora.catalog.auth.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Repository
public class RefreshTokenRepository {

    private final JdbcTemplate jdbc;

    public RefreshTokenRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(long usuarioId, String tokenHash, Instant expiraEn) {
        jdbc.update("INSERT INTO main.refresh_token (usuario_id, token_hash, expira_en) VALUES (?, ?, ?)",
                usuarioId, tokenHash, Timestamp.from(expiraEn));
    }

    /**
     * Revoca el token si sigue vigente y devuelve su usuario. En una sola sentencia para que dos
     * refresh simultáneos con el mismo token no obtengan los dos una sesión nueva.
     */
    public Optional<Long> consume(String tokenHash, Instant now) {
        return jdbc.queryForList("""
                UPDATE main.refresh_token SET revocado = TRUE
                 WHERE token_hash = ? AND NOT revocado AND expira_en > ?
                RETURNING usuario_id
                """, Long.class, tokenHash, Timestamp.from(now)).stream().findFirst();
    }

    public void revoke(String tokenHash) {
        jdbc.update("UPDATE main.refresh_token SET revocado = TRUE WHERE token_hash = ?", tokenHash);
    }

    public int revokeAllByUsuario(long usuarioId) {
        return jdbc.update("UPDATE main.refresh_token SET revocado = TRUE WHERE usuario_id = ? AND NOT revocado",
                usuarioId);
    }

    public int revokeAllByEmpresa(long empresaId) {
        return jdbc.update("""
                UPDATE main.refresh_token SET revocado = TRUE
                 WHERE NOT revocado AND usuario_id IN (SELECT id FROM main.usuario WHERE empresa_id = ?)
                """, empresaId);
    }
}
