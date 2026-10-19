package com.agora.catalog.auth.dto.response;

/** @param expiresIn segundos de vida del access token */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        SesionUsuarioResponse usuario
) {
}
