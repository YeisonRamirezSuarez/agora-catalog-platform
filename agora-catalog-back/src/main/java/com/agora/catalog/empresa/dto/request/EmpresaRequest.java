package com.agora.catalog.empresa.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** @param slug opcional al crear: si falta, sale del nombre */
public record EmpresaRequest(
        @NotBlank(message = "Escribe el nombre del negocio")
        @Size(max = 120, message = "El nombre puede tener hasta 120 caracteres")
        String nombre,

        @Size(max = 80, message = "El slug puede tener hasta 80 caracteres")
        @Pattern(regexp = "^$|^[a-z0-9]+(-[a-z0-9]+)*$",
                message = "El slug solo lleva minúsculas, números y guiones (ej. salsamentaria-javier)")
        String slug
) {
}
