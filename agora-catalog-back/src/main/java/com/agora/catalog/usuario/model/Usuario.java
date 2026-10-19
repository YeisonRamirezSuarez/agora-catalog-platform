package com.agora.catalog.usuario.model;

/**
 * Fila de {@code usuario} con lo que el login necesita de su empresa.
 *
 * @param empresaActiva {@code true} para SUPER_ADMIN, que no tiene empresa
 */
public record Usuario(
        long id,
        Long empresaId,
        String empresaNombre,
        boolean empresaActiva,
        String nombre,
        String email,
        String passwordHash,
        Rol rol,
        boolean activo,
        boolean debeCambiarPassword
) {

    /** Puede entrar: usuario activo y, si tiene empresa, empresa activa. */
    public boolean puedeIngresar() {
        return activo && empresaActiva;
    }
}
