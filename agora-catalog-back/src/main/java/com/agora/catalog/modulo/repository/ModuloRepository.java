package com.agora.catalog.modulo.repository;

import com.agora.catalog.modulo.model.Modulo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.EnumSet;
import java.util.Set;

/** Módulos encendidos de cada empresa. */
@Repository
public class ModuloRepository {

    private final JdbcTemplate jdbc;

    public ModuloRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Set<Modulo> activos(long empresaId) {
        Set<Modulo> activos = EnumSet.noneOf(Modulo.class);
        jdbc.queryForList("SELECT modulo FROM main.empresa_modulo WHERE empresa_id = ? AND activo", String.class, empresaId)
                .forEach(m -> activos.add(Modulo.valueOf(m)));
        return activos;
    }

    public boolean tiene(long empresaId, Modulo modulo) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM main.empresa_modulo WHERE empresa_id = ? AND modulo = ? AND activo)",
                Boolean.class, empresaId, modulo.name()));
    }

    /** Enciende o apaga uno. Apagar deja la fila (y los datos del módulo) intactos. */
    public void set(long empresaId, Modulo modulo, boolean activo) {
        jdbc.update("""
                INSERT INTO main.empresa_modulo (empresa_id, modulo, activo) VALUES (?, ?, ?)
                ON CONFLICT (empresa_id, modulo) DO UPDATE SET activo = EXCLUDED.activo, updated_at = now()
                """, empresaId, modulo.name(), activo);
    }
}
