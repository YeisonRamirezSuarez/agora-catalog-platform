package com.agora.catalog.common.dto;

import jakarta.validation.constraints.NotNull;

/** Cuerpo de los PATCH que encienden o apagan algo (empresa activa, usuario activo). */
public record ActivoRequest(
        @NotNull(message = "Falta indicar si queda activo")
        Boolean activo
) {
}
