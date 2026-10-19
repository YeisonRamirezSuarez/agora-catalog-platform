package com.agora.catalog.common.secrets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.List;
import java.util.Map;

/**
 * Resuelve los secretos en el arranque y los deja disponibles como propiedades, de modo que
 * {@code ${DB_PASSWORD}} y compañía funcionen igual venga el valor de donde venga.
 *
 * <p>El origen se elige con {@code settings.secrets.provider}; si no se dice nada, es
 * {@code env}. Un nombre desconocido detiene el arranque en vez de seguir sin secretos.
 *
 * <p>Se agrega con {@code addLast}, así que <strong>lo que ya esté definido en el entorno
 * gana</strong>: una variable del sistema o un {@code -D} tienen la última palabra sobre el
 * archivo. Eso permite sobrescribir un valor puntual sin tocar el origen.
 */
public class SecretsEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROVIDER_PROPERTY = "settings.secrets.provider";
    private static final String DEFAULT_PROVIDER = "env";
    private static final String PROPERTY_SOURCE_NAME = "secrets";

    private static final List<SecretsSource> SOURCES = List.of(new EnvFileSecretsSource());

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String provider = environment.getProperty(PROVIDER_PROPERTY, DEFAULT_PROVIDER).trim();

        SecretsSource source = SOURCES.stream()
            .filter(candidate -> candidate.name().equalsIgnoreCase(provider))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Origen de secretos desconocido en " + PROVIDER_PROPERTY + ": '" + provider
                    + "'. Los válidos son: " + SOURCES.stream().map(SecretsSource::name).toList()));

        Map<String, Object> secrets = source.load(environment);
        if (!secrets.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, secrets));
        }
    }

}
