package com.agora.catalog.pedidotienda.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Entrada y salida de /admin/pedidos-tienda: lo que se le vende a una tienda desde un carro. */
public final class PedidoTiendaDtos {

    private PedidoTiendaDtos() {
    }

    public record LineaPedido(
            @NotNull(message = "Falta el producto")
            Long productoId,

            @Size(max = 60, message = "La presentación puede tener hasta 60 caracteres")
            String presentacion,

            @NotNull(message = "Falta la cantidad")
            @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor que cero")
            BigDecimal cantidad,

            /* Opcional: sin precio se cobra el sugerido (el de catálogo). */
            @Min(value = 0, message = "El precio no puede ser negativo")
            @Max(value = 999_999_999, message = "El precio es demasiado grande")
            Long precioUnitario
    ) {
    }

    /** Corregir un pedido: las líneas nuevas reemplazan a las anteriores (el carro no cambia). */
    public record EditarPedidoRequest(
            @NotBlank(message = "Escribe el nombre de la tienda")
            @Size(max = 80, message = "El nombre de la tienda puede tener hasta 80 caracteres")
            String tiendaNombre,

            @NotEmpty(message = "El pedido debe tener al menos un producto")
            List<@Valid LineaPedido> items,

            @Size(max = 300, message = "Las notas pueden tener hasta 300 caracteres")
            String notas
    ) {
    }

    /** La tienda es solo un nombre: queda en el pedido y no se guarda en ningún otro lado. */
    public record PedidoRequest(
            @NotNull(message = "Elige el carro")
            Long carroId,

            @NotBlank(message = "Escribe el nombre de la tienda")
            @Size(max = 80, message = "El nombre de la tienda puede tener hasta 80 caracteres")
            String tiendaNombre,

            @NotEmpty(message = "Agrega al menos un producto")
            List<@Valid LineaPedido> items,

            @Size(max = 300, message = "Las notas pueden tener hasta 300 caracteres")
            String notas
    ) {
    }

    public record ItemResponse(
            Long productoId,
            String productoNombre,
            String presentacion,
            String unidadVenta,
            BigDecimal cantidad,
            long precioSugerido,
            long precioUnitario,
            long subtotal
    ) {
    }

    public record PedidoResponse(
            long id,
            int numero,
            long carroId,
            String carroNombre,
            /* El carro se eliminó después: el comprobante lo nombra, pero ya no hay a dónde ir. */
            boolean carroEliminado,
            String tiendaNombre,
            String formaPago,
            long total,
            String estado,
            String notas,
            String usuarioNombre,
            OffsetDateTime createdAt,
            /* null si nunca se editó. */
            OffsetDateTime editadoAt,
            List<ItemResponse> items
    ) {
    }
}
