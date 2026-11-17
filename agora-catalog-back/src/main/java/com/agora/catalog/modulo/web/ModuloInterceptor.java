package com.agora.catalog.modulo.web;

import com.agora.catalog.auth.model.UsuarioPrincipal;
import com.agora.catalog.modulo.repository.ModuloRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Antes de atender una ruta marcada con {@link RequiereModulo}, revisa que la empresa del token
 * tenga ese módulo encendido. Se consulta en cada petición: apagar un módulo corta el acceso al
 * instante, sin esperar a que venza el token.
 */
@Component
public class ModuloInterceptor implements HandlerInterceptor {

    private final ModuloRepository modulos;

    public ModuloInterceptor(ModuloRepository modulos) {
        this.modulos = modulos;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod metodo)) return true;
        RequiereModulo requiere = AnnotatedElementUtils.findMergedAnnotation(metodo.getMethod(), RequiereModulo.class);
        if (requiere == null) requiere = AnnotatedElementUtils.findMergedAnnotation(metodo.getBeanType(), RequiereModulo.class);
        if (requiere == null) return true;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Sin empresa (anónimo o super admin) no hay módulo que revisar: @PreAuthorize ya responde 401/403.
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioPrincipal u) || u.empresaId() == null) return true;
        if (!modulos.tiene(u.empresaId(), requiere.value())) throw new ModuloInactivoException(requiere.value());
        return true;
    }
}
