package com.agora.catalog.empresa.dto.response;

/** La contraseña temporal sale una sola vez, en esta respuesta; la base solo guarda su hash. */
public record AdminCreadoResponse(long id, String nombre, String email, String passwordTemporal) {
}
