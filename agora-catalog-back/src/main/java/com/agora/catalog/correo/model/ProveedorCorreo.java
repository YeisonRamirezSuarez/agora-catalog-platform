package com.agora.catalog.correo.model;

import java.time.OffsetDateTime;

/** Fila única de {@code proveedor_correo}. La contraseña viaja cifrada: se descifra solo al enviar. */
public record ProveedorCorreo(
        String host,
        int puerto,
        String usuario,
        String passwordCifrada,
        String remitenteEmail,
        String remitenteNombre,
        Seguridad seguridad,
        boolean activo,
        OffsetDateTime updatedAt
) {

    public enum Seguridad {
        STARTTLS,
        SSL,
        NINGUNA
    }
}
