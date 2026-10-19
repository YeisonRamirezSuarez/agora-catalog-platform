package com.agora.catalog.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "settings.flyway.enabled", havingValue = "true", matchIfMissing = false)
public class FlywayConfig {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    /**
     * Permite aplicar una migración cuya versión es menor que la última ya registrada.
     *
     * <p>Apagado por defecto, y así debe quedar en cualquier ambiente compartido: con esto en
     * true, una migración que llega tarde se aplica sin ruido y el esquema deja de ser
     * reproducible desde cero. En local sí hace falta: al traer migraciones de otros después de
     * haber probado las propias, Flyway se niega a arrancar.</p>
     */
    @Value("${settings.flyway.out-of-order:false}")
    private boolean outOfOrder;

    @Bean(initMethod = "migrate")
    public Flyway flyway() {
        return Flyway.configure()
                .dataSource(url, username, password)
                .schemas("main")
                .defaultSchema("main")
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .outOfOrder(outOfOrder)
                .load();
    }
}
