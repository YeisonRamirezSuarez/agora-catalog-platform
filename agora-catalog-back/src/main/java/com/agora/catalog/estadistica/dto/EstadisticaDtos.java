package com.agora.catalog.estadistica.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.List;

/** Entrada del catálogo público y salida del panel de /estadisticas. */
public final class EstadisticaDtos {

    private EstadisticaDtos() {
    }

    /** {@code productoId} solo en PRODUCTO; en los demás tipos se ignora. */
    public record EventoRequest(
            @NotNull(message = "Falta el tipo")
            @Pattern(regexp = "^(VISITA|PRODUCTO|PEDIDO|WHATSAPP)$", message = "Tipo de evento no válido")
            String tipo,
            Long productoId
    ) {
    }

    public record Dia(LocalDate fecha, int visitas, int productosVistos, int pedidos, int whatsapp) {
    }

    public record ProductoVisto(long id, String nombre, String imagenThumbUrl, int vistas) {
    }

    /** Totales del período, un punto por día (con ceros) y los productos más vistos. */
    public record EstadisticasResponse(
            int dias,
            int visitas,
            int pedidos,
            int whatsapp,
            List<Dia> porDia,
            List<ProductoVisto> masVistos
    ) {
    }
}
