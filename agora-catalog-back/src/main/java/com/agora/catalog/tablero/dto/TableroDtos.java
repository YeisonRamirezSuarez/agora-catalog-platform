package com.agora.catalog.tablero.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Salida de /admin/tablero-ruta: cómo le fue a la venta en ruta un día. */
public final class TableroDtos {

    private TableroDtos() {
    }

    public record TableroResponse(
            LocalDate fecha,
            /* Plata de los pedidos del día (sin anulados). */
            long vendido,
            int pedidos,
            List<RutaDia> rutas,
            /* Lo más vendido del día, de más plata a menos. */
            List<ProductoVendido> productos,
            /* Los pedidos del día, del más reciente al más viejo. */
            List<PedidoDia> pedidosDia
    ) {
    }

    /** Una ruta del día. {@code numero}: 1.ª, 2.ª... ruta del carro en el día en que salió. */
    public record RutaDia(
            long id,
            long carroId,
            String carroNombre,
            boolean carroEliminado,
            int numero,
            OffsetDateTime iniciadaAt,
            OffsetDateTime cerradaAt,
            int pedidos,
            long vendido
    ) {
    }

    public record ProductoVendido(
            String nombre,
            String presentacion,
            String unidadVenta,
            BigDecimal cantidad,
            long total
    ) {
    }

    public record PedidoDia(
            long id,
            int numero,
            String tiendaNombre,
            String carroNombre,
            long total,
            String estado,
            OffsetDateTime createdAt
    ) {
    }
}
