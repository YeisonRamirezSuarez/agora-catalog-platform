package com.agora.catalog.testing;

import com.agora.catalog.modulo.model.Modulo;
import org.springframework.jdbc.core.JdbcTemplate;

/** Una empresa creada en una prueba arranca sin módulos; las pruebas de un módulo se los encienden. */
public final class Modulos {

    private Modulos() {
    }

    public static void todos(JdbcTemplate jdbc, long empresaId) {
        for (Modulo m : Modulo.values()) {
            jdbc.update("INSERT INTO main.empresa_modulo (empresa_id, modulo) VALUES (?, ?) ON CONFLICT DO NOTHING", empresaId, m.name());
        }
    }
}
