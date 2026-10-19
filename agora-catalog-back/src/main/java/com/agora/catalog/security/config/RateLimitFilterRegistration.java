package com.agora.catalog.security.config;

import com.agora.catalog.security.filter.RateLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Un {@code @Component} que extiende {@code OncePerRequestFilter} lo registra Spring Boot solo
 * en la cadena de servlets, además de donde lo pongamos a mano. Acá se desactiva ese registro
 * automático para que {@link RateLimitFilter} corra únicamente dentro de la cadena de seguridad,
 * después del filtro de CORS: afuera, un 429 saldría sin cabeceras de CORS.
 */
@Configuration
public class RateLimitFilterRegistration {

    @Bean
    // El nombre del método NO puede ser `rateLimitFilterRegistration`: coincidiría con el nombre
    // del bean de esta clase y el arranque falla con BeanDefinitionOverrideException.
    public FilterRegistrationBean<RateLimitFilter> disableRateLimitFilterAutoRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

}
