package com.agora.catalog.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Toda ruta de {@code /admin} y {@code /superadmin} declara su rol con {@code @PreAuthorize}. Sin
 * esto, una ruta nueva olvidada quedaría abierta a cualquier usuario con sesión, de cualquier rol.
 */
@SpringBootTest
class AuthorizationCoverageTest {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mapping;

    @Test
    void todaRutaPrivadaLlevaPreAuthorize() {
        List<String> sinRol = new ArrayList<>();
        mapping.getHandlerMethods().forEach((info, handler) -> {
            boolean privada = info.getPatternValues().stream()
                    .anyMatch(path -> path.startsWith("/admin") || path.startsWith("/superadmin"));
            if (privada && !tienePreAuthorize(handler)) {
                sinRol.add(info + " -> " + handler);
            }
        });
        assertThat(sinRol).as("Rutas privadas sin @PreAuthorize").isEmpty();
    }

    private static boolean tienePreAuthorize(HandlerMethod handler) {
        return AnnotatedElementUtils.hasAnnotation(handler.getMethod(), PreAuthorize.class)
                || AnnotatedElementUtils.hasAnnotation(handler.getBeanType(), PreAuthorize.class);
    }
}
