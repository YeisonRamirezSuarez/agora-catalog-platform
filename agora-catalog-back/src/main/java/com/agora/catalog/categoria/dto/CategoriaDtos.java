package com.agora.catalog.categoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Entrada y salida de /admin/categorias. */
public final class CategoriaDtos {

    private CategoriaDtos() {
    }

    public record CategoriaRequest(
            @NotBlank(message = "Escribe el nombre de la categoría")
            @Size(max = 60, message = "El nombre puede tener hasta 60 caracteres")
            String nombre,

            @NotNull(message = "Indica si la categoría se ve en el catálogo")
            Boolean activa
    ) {
    }

    public record OrdenRequest(
            @NotEmpty(message = "Falta el orden de las categorías")
            List<Long> ids
    ) {
    }

    /** La franja grande del inicio: texto en el formulario y foto aparte (multipart). */
    public record FranjaRequest(
            @Size(max = 40, message = "El título puede tener hasta 40 caracteres") String titulo,
            @Size(max = 80, message = "La frase puede tener hasta 80 caracteres") String frase,
            @Size(max = 32, message = "La nota puede tener hasta 32 caracteres") String nota
    ) {
    }

    public record CategoriaResponse(
            long id,
            String nombre,
            int orden,
            boolean activa,
            String imagenUrl,
            int productos,
            boolean bannerActivo,
            String bannerUrl,
            String bannerTitulo,
            String bannerFrase,
            String bannerNota
    ) {
    }
}
