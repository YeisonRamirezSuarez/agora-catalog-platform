package com.agora.catalog.tablero.service;

import com.agora.catalog.tablero.dto.TableroDtos.PedidoDia;
import com.agora.catalog.tablero.dto.TableroDtos.TableroResponse;
import com.agora.catalog.tablero.repository.TableroRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/** El tablero de un día de venta en ruta: lo vendido, las rutas de cada carro, lo más vendido y los pedidos. */
@Service
public class TableroRutaService {

    private final TableroRepository repository;
    private final Clock clock;

    public TableroRutaService(TableroRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** {@code fecha} null: hoy, en la zona horaria del negocio. */
    public TableroResponse dia(long empresaId, LocalDate fecha) {
        ZoneId zona = ZoneId.systemDefault();
        LocalDate dia = fecha != null ? fecha : LocalDate.now(clock.withZone(zona));
        OffsetDateTime desde = dia.atStartOfDay(zona).toOffsetDateTime();
        OffsetDateTime hasta = dia.plusDays(1).atStartOfDay(zona).toOffsetDateTime();

        List<PedidoDia> pedidos = repository.pedidos(empresaId, desde, hasta);
        List<PedidoDia> confirmados = pedidos.stream().filter(p -> "CONFIRMADO".equals(p.estado())).toList();
        return new TableroResponse(dia, confirmados.stream().mapToLong(PedidoDia::total).sum(), confirmados.size(),
                repository.rutas(empresaId, desde, hasta, zona.getId()), repository.productos(empresaId, desde, hasta, 10),
                pedidos);
    }
}
