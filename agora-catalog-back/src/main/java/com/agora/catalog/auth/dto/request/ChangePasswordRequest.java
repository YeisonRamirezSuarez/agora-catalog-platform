package com.agora.catalog.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Escribe tu contraseña actual")
        @Size(max = 72, message = "La contraseña actual es demasiado larga")
        String actual,

        // BCrypt ignora lo que pase de 72 bytes: más largo daría una falsa sensación de seguridad.
        @NotBlank(message = "Escribe la contraseña nueva")
        @Size(min = 8, max = 72, message = "La contraseña nueva debe tener entre 8 y 72 caracteres")
        String nueva
) {
}
