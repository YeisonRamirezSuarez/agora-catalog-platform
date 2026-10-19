package com.agora.catalog.common.time;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Fija la zona horaria por defecto de la JVM en la del proyecto ({@code settings.time-zone})
 * antes de que se cree el pool de conexiones.
 *
 * <p>Con eso quedan en la misma hora el {@code LocalDate.now()} del backend (oferta vigente,
 * producto nuevo), la sesión de cada conexión a PostgreSQL (el driver la abre con la zona de la
 * JVM) y los {@code timestamptz} que se leen como {@code LocalDateTime}, corra donde corra el
 * servidor.
 *
 * <p>Va después de los secretos para que un {@code APP_TIME_ZONE} del {@code .env} cuente. Una
 * zona que no existe detiene el arranque: {@link TimeZone#getTimeZone(String)} la cambiaría por
 * GMT en silencio.
 */
public class TimeZoneEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY = "settings.time-zone";
    static final String DEFAULT_ZONE = "America/Bogota";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String zone = environment.getProperty(PROPERTY, DEFAULT_ZONE).trim();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(zone)));
        } catch (RuntimeException e) {
            throw new IllegalStateException("Zona horaria inválida en " + PROPERTY + ": '" + zone
                + "' (se espera un nombre IANA, ej. 'America/Bogota')", e);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}
