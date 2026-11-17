package com.agora.catalog.tienda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Entrada y salida de /admin/tiendas (venta en ruta). */
public final class TiendaDtos {

    private TiendaDtos() {
    }

    public record TiendaRequest(
            @NotBlank(message = "Escribe el nombre de la tienda")
            @Size(max = 80, message = "El nombre puede tener hasta 80 caracteres")
            String nombre,

            @Size(max = 80, message = "El encargado puede tener hasta 80 caracteres")
            String encargado,

            @Pattern(regexp = "^[0-9 +()-]{0,25}$", message = "El teléfono solo lleva números")
            String telefono,

            @Size(max = 150, message = "La dirección puede tener hasta 150 caracteres")
            String direccion,

            @Size(max = 80, message = "El barrio puede tener hasta 80 caracteres")
            String barrio,

            @Size(max = 300, message = "Las notas pueden tener hasta 300 caracteres")
            String notas,

            @NotNull(message = "Indica si la tienda está activa")
            Boolean activa
    ) {
    }

    public record TiendaResponse(
            long id,
            String nombre,
            String encargado,
            String telefono,
            String direccion,
            String barrio,
            String notas,
            boolean activa,
            int pedidos
    ) {
    }
}
