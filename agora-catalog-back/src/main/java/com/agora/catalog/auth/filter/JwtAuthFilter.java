package com.agora.catalog.auth.filter;

import com.agora.catalog.auth.model.UsuarioPrincipal;
import com.agora.catalog.auth.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lee el {@code Authorization: Bearer} y deja al usuario autenticado con su rol. Un token inválido
 * o vencido no es un error del servidor: se sigue sin autenticar y la ruta protegida responde 401.
 */
@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                UsuarioPrincipal principal = jwtService.parse(header.substring(7));
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(principal, null, principal.authorities()));
            } catch (JwtException | IllegalArgumentException exception) {
                log.debug("[AUTH] Token rechazado en {} {}: {}",
                        request.getMethod(), request.getRequestURI(), exception.getMessage());
            }
        }
        chain.doFilter(request, response);
    }
}
