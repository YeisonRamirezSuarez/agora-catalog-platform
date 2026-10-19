package com.agora.catalog.coleccion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Entrada y salida de /admin/colecciones (los planes de «¿Cuál es el plan?»). */
public final class ColeccionDtos {

    private ColeccionDtos() {
    }

    public record ColeccionRequest(
            @NotBlank(message = "Escribe el nombre de la colección")
            @Size(max = 40, message = "El nombre puede tener hasta 40 caracteres") String nombre,
            @Size(max = 80, message = "La descripción puede tener hasta 80 caracteres") String descripcion,
            @NotNull(message = "Indica si la colección se ve en el catálogo") Boolean visible,
            @NotNull(message = "Faltan los productos") @Size(max = 200, message = "Una colección puede tener hasta 200 productos")
            List<Long> productos
    ) {
    }

    public record SeccionRequest(
            @NotBlank(message = "Escribe el título de la sección")
            @Size(max = 40, message = "El título puede tener hasta 40 caracteres") String titulo,
            @Size(max = 80, message = "El texto puede tener hasta 80 caracteres") String texto,
            @Size(max = 40, message = "La nota puede tener hasta 40 caracteres") String nota
    ) {
    }

    public record OrdenRequest(@NotEmpty(message = "Falta el orden de las colecciones") List<Long> ids) {
    }

    public record ColeccionResponse(long id, String nombre, String descripcion, String imagenUrl, boolean visible,
                                    int orden, List<Long> productos) {
    }
}
