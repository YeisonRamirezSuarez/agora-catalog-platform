package com.agora.catalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Reloj inyectable: las reglas de fecha (vencimiento de sesión, oferta vigente) se prueban fijándolo. */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        // La zona por defecto ya es la del proyecto (TimeZoneEnvironmentPostProcessor).
        return Clock.systemDefaultZone();
    }
}
