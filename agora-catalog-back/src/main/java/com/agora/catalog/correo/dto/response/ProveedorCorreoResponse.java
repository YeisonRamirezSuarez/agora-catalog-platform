package com.agora.catalog.correo.dto.response;

import com.agora.catalog.correo.model.ProveedorCorreo;

import java.time.OffsetDateTime;

/** La configuración sin la contraseña: solo se dice si hay una guardada. */
public record ProveedorCorreoResponse(
        String host,
        int puerto,
        String usuario,
        boolean passwordGuardada,
        String remitenteEmail,
        String remitenteNombre,
        ProveedorCorreo.Seguridad seguridad,
        boolean activo,
        OffsetDateTime updatedAt
) {

    public static ProveedorCorreoResponse of(ProveedorCorreo p) {
        return new ProveedorCorreoResponse(p.host(), p.puerto(), p.usuario(), p.passwordCifrada() != null,
                p.remitenteEmail(), p.remitenteNombre(), p.seguridad(), p.activo(), p.updatedAt());
    }
}
