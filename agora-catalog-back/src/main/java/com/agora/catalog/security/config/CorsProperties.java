package com.agora.catalog.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "settings.cors")
@Getter
@Setter
public class CorsProperties {
    private List<String> allowedOrigins;
}
