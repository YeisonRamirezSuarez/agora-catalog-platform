package com.agora.catalog.empresa.dto.response;

/**
 * La contraseña temporal sale una sola vez, en esta respuesta; la base solo guarda su hash.
 *
 * @param correoEnviado si se le enviaron los datos por correo
 * @param correoError   por qué no salió el correo, si se pidió y falló
 */
public record AdminCreadoResponse(long id, String nombre, String email, String passwordTemporal,
                                  boolean correoEnviado, String correoError) {

    public AdminCreadoResponse withCorreo(boolean enviado, String error) {
        return new AdminCreadoResponse(id, nombre, email, passwordTemporal, enviado, error);
    }
}
