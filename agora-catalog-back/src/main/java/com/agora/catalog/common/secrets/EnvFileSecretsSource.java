package com.agora.catalog.common.secrets;

import org.springframework.core.env.ConfigurableEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Origen local: el archivo {@code .env} del directorio de trabajo (la raíz del backend).
 *
 * <p>No falla si el archivo no existe: un entorno puede tener los valores como variables del
 * sistema (Docker, CI), y ese caso ya lo cubre Spring por su cuenta.
 */
public class EnvFileSecretsSource implements SecretsSource {

    private static final Path DOTENV = Path.of(".env");

    @Override
    public String name() {
        return "env";
    }

    @Override
    public Map<String, Object> load(ConfigurableEnvironment environment) {
        if (!Files.exists(DOTENV)) {
            return Map.of();
        }
        Map<String, Object> secrets = new HashMap<>();
        try {
            for (String line : Files.readAllLines(DOTENV)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                int eq = trimmed.indexOf('=');
                if (eq <= 0) continue;

                String key = trimmed.substring(0, eq).trim();
                String value = unquote(trimmed.substring(eq + 1).trim());
                secrets.put(key, value);
            }
        } catch (IOException exception) {
            // Un .env ilegible no debe tumbar el arranque en local, donde lo habitual es que
            // falte o esté a medio llenar.
            return Map.of();
        }
        return secrets;
    }

    private String unquote(String value) {
        boolean quoted = (value.startsWith("'") && value.endsWith("'"))
                      || (value.startsWith("\"") && value.endsWith("\""));
        return quoted && value.length() >= 2 ? value.substring(1, value.length() - 1) : value;
    }

}
