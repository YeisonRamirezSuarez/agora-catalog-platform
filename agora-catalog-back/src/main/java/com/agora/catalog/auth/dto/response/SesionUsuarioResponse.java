package com.agora.catalog.auth.dto.response;

import com.agora.catalog.usuario.model.Rol;
import com.agora.catalog.usuario.model.Usuario;

/** Lo que el front necesita saber de quien inició sesión. */
public record SesionUsuarioResponse(
        long id,
        String nombre,
        String email,
        Rol rol,
        Long empresaId,
        String empresaNombre,
        boolean debeCambiarPassword
) {

    public static SesionUsuarioResponse of(Usuario usuario) {
        return new SesionUsuarioResponse(usuario.id(), usuario.nombre(), usuario.email(), usuario.rol(),
                usuario.empresaId(), usuario.empresaNombre(), usuario.debeCambiarPassword());
    }
}
