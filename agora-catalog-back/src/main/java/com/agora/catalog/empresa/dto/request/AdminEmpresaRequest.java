package com.agora.catalog.empresa.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminEmpresaRequest(
        @NotBlank(message = "Escribe el nombre del administrador")
        @Size(max = 120, message = "El nombre puede tener hasta 120 caracteres")
        String nombre,

        @NotBlank(message = "Escribe el correo del administrador")
        @Email(message = "El correo no es válido")
        @Size(max = 160, message = "El correo puede tener hasta 160 caracteres")
        String email
) {
}
