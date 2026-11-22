package com.agora.catalog.carro.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Entrada y salida de /admin/carros (venta en ruta): carros, su stock y sus movimientos. */
public final class CarroDtos {

    private CarroDtos() {
    }

    public record CarroRequest(
            @NotBlank(message = "Escribe el nombre del carro")
            @Size(max = 60, message = "El nombre puede tener hasta 60 caracteres")
            String nombre,

            @Size(max = 15, message = "La placa puede tener hasta 15 caracteres")
            String placa,

            @NotNull(message = "Indica si el carro está activo")
            Boolean activo
    ) {
    }

    /** Una línea de carga: cuánto de un producto (y presentación) entra al carro. */
    public record LineaCarga(
            @NotNull(message = "Falta el producto")
            Long productoId,

            @Size(max = 60, message = "La presentación puede tener hasta 60 caracteres")
            String presentacion,

            @NotNull(message = "Falta la cantidad")
            @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor que cero")
            BigDecimal cantidad
    ) {
    }

    public record CargaRequest(
            @NotEmpty(message = "Agrega al menos un producto")
            List<@Valid LineaCarga> items,

            @Size(max = 200, message = "La nota puede tener hasta 200 caracteres")
            String nota
    ) {
    }

    /** Una línea de ajuste: la cantidad que de verdad hay (contada), no la diferencia. */
    public record LineaAjuste(
            @NotNull(message = "Falta el producto")
            Long productoId,

            @Size(max = 60, message = "La presentación puede tener hasta 60 caracteres")
            String presentacion,

            @NotNull(message = "Falta la cantidad")
            @DecimalMin(value = "0", message = "La cantidad no puede ser negativa")
            BigDecimal cantidad
    ) {
    }

    public record AjusteRequest(
            @NotEmpty(message = "Agrega al menos un producto")
            List<@Valid LineaAjuste> items,

            /* Opcional: cambiar la cantidad de un producto no pide explicaciones. */
            @Size(max = 200, message = "El motivo puede tener hasta 200 caracteres")
            String motivo
    ) {
    }

    public record DescargueRequest(
            @Size(max = 200, message = "La nota puede tener hasta 200 caracteres")
            String nota
    ) {
    }

    public record CarroResponse(
            long id,
            String nombre,
            String placa,
            boolean activo,
            /* Cuántos productos distintos lleva. */
            int productos,
            /* Lo que vale lo que lleva, a precio de catálogo. */
            long valor
    ) {
    }

    public record StockItem(
            long productoId,
            String codigo,
            String nombre,
            String presentacion,
            String unidadVenta,
            BigDecimal cantidad,
            /* Precio de catálogo de esa presentación. */
            long precio,
            String imagenThumbUrl
    ) {
    }

    public record CarroDetalleResponse(
            long id,
            String nombre,
            String placa,
            boolean activo,
            int productos,
            long valor,
            List<StockItem> stock
    ) {
    }

    public record MovimientoResponse(
            long id,
            String tipo,
            Long productoId,
            String productoNombre,
            String presentacion,
            BigDecimal cantidad,
            BigDecimal saldo,
            String motivo,
            Long pedidoTiendaId,
            String usuarioNombre,
            OffsetDateTime createdAt
    ) {
    }

    /** Empezar una ruta nueva: lo que sobró de la anterior sigue en el carro (true) o se baja (false). */
    public record NuevaRutaRequest(
            @NotNull(message = "Indica si lo que sobró sigue en el carro")
            Boolean llevarSobrante
    ) {
    }

    /** Una salida del carro con lo que pasó con cada producto. {@code numero}: 1.ª, 2.ª... ruta de ese día. */
    public record RutaResponse(
            long id,
            int numero,
            OffsetDateTime iniciadaAt,
            /* null: es la ruta en curso. */
            OffsetDateTime cerradaAt,
            List<RutaItem> items,
            /* Los pedidos que se hicieron en esta ruta, del primero al último. */
            List<PedidoRuta> pedidos
    ) {
    }

    /** Un pedido hecho en la ruta, para listarlo y abrir su comprobante. */
    public record PedidoRuta(
            long id,
            int numero,
            String tiendaNombre,
            long total,
            String estado,
            OffsetDateTime createdAt
    ) {
    }

    /** Un producto en una ruta: traía de la anterior + cargó − vendió − quitó = queda. */
    public record RutaItem(
            Long productoId,
            String nombre,
            String presentacion,
            String unidadVenta,
            BigDecimal traia,
            BigDecimal cargado,
            BigDecimal vendido,
            BigDecimal quitado,
            BigDecimal queda
    ) {
    }
}
