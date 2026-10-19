package com.agora.catalog.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(
        @NotBlank(message = "Falta el refresh token")
        @Size(max = 100, message = "El refresh token no es válido")
        String refreshToken
) {
}
