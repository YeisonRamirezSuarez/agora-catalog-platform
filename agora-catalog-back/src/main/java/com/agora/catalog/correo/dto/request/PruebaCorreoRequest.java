package com.agora.catalog.correo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PruebaCorreoRequest(
        @NotBlank(message = "Escribe a qué correo enviar la prueba")
        @Email(message = "El correo no es válido")
        @Size(max = 160, message = "El correo puede tener hasta 160 caracteres")
        String destinatario
) {
}
