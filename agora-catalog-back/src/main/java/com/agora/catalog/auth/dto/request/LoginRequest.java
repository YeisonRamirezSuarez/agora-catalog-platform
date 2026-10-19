package com.agora.catalog.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Escribe tu correo")
        @Size(max = 160, message = "El correo es demasiado largo")
        String email,

        @NotBlank(message = "Escribe tu contraseña")
        @Size(max = 72, message = "La contraseña es demasiado larga")
        String password
) {
}
