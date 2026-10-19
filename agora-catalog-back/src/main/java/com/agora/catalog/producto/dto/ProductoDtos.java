package com.agora.catalog.producto.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Entrada y salida de /admin/productos. */
public final class ProductoDtos {

    private ProductoDtos() {
    }

    /** Lo mismo que trae una fila del Excel: un solo validador para el formulario y la importación. */
    public record ProductoRequest(
            @NotBlank(message = "Escribe el código")
            @Size(max = 40, message = "El código puede tener hasta 40 caracteres")
            @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "El código solo lleva letras, números, punto, guion y guion bajo")
            String codigo,

            @NotNull(message = "Elige la categoría")
            Long categoriaId,

            @NotBlank(message = "Escribe el nombre")
            @Size(max = 120, message = "El nombre puede tener hasta 120 caracteres")
            String nombre,

            @Size(max = 500, message = "La descripción puede tener hasta 500 caracteres")
            String descripcion,

            @Size(max = 60, message = "La presentación puede tener hasta 60 caracteres")
            String presentacion,

            @NotBlank(message = "Elige cómo se vende")
            @Pattern(regexp = "^(UNIDAD|LIBRA|KILO|GRAMOS|PAQUETE|CAJA)$", message = "La unidad de venta no es válida")
            String unidadVenta,

            @NotNull(message = "Escribe el precio")
            @Min(value = 0, message = "El precio no puede ser negativo")
            @Max(value = 999_999_999, message = "El precio es demasiado alto")
            Long precio,

            @Min(value = 0, message = "El precio de oferta no puede ser negativo")
            Long precioOferta,

            LocalDate ofertaDesde,
            LocalDate ofertaHasta,

            @NotNull(message = "Indica si está disponible")
            Boolean disponible,

            @NotNull(message = "Indica si es «Más vendido»")
            Boolean destacado
    ) {
    }

    public record DisponibleRequest(@NotNull(message = "Indica si está disponible") Boolean disponible) {
    }

    public record CambioPrecio(
            @NotNull(message = "Falta el producto") Long id,
            @NotNull(message = "Falta el precio")
            @Min(value = 0, message = "El precio no puede ser negativo")
            @Max(value = 999_999_999, message = "El precio es demasiado alto")
            Long precio
    ) {
    }

    public record PreciosRequest(
            @NotEmpty(message = "No hay precios para guardar")
            @Size(max = 500, message = "Guarda hasta 500 precios a la vez")
            List<@Valid CambioPrecio> cambios
    ) {
    }

    public record ProductoResponse(
            long id,
            String codigo,
            long categoriaId,
            String categoriaNombre,
            String nombre,
            String descripcion,
            String presentacion,
            String unidadVenta,
            BigDecimal precio,
            BigDecimal precioOferta,
            LocalDate ofertaDesde,
            LocalDate ofertaHasta,
            boolean ofertaVigente,
            String imagenUrl,
            String imagenThumbUrl,
            boolean disponible,
            boolean destacado,
            int orden
    ) {
    }

    public record DashboardResponse(
            int productos,
            int sinFoto,
            int agotados,
            int ofertasVigentes,
            String linkPublico
    ) {
    }
}
