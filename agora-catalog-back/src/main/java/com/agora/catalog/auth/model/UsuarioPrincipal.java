package com.agora.catalog.auth.model;

import com.agora.catalog.usuario.model.Rol;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * Quién hace la petición, tal como viene firmado en el token. Los controladores lo reciben con
 * {@code @AuthenticationPrincipal}: el {@code empresaId} sale de acá y nunca del request.
 *
 * @param empresaId null para SUPER_ADMIN
 */
public record UsuarioPrincipal(long id, Long empresaId, Rol rol) {

    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }
}
