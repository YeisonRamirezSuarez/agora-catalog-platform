package com.agora.catalog.estadistica.service;

import com.agora.catalog.estadistica.dto.EstadisticaDtos.Dia;
import com.agora.catalog.estadistica.dto.EstadisticaDtos.EstadisticasResponse;
import com.agora.catalog.estadistica.repository.EstadisticaRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Estadísticas del catálogo: el público suma contadores por día y el panel los lee. No se guarda
 * nada de quien visita (ni IP ni navegador): solo cuántas veces pasó cada cosa.
 */
@Service
public class EstadisticaService {

    static final int MAS_VISTOS = 5;

    private final EstadisticaRepository repository;
    private final Clock clock;

    public EstadisticaService(EstadisticaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Un catálogo inexistente o desactivado no cuenta nada y no da error: el visitante no se entera. */
    public void registrar(String publicHash, String tipo, Long productoId) {
        repository.empresaActiva(publicHash).ifPresent(empresaId -> {
            LocalDate hoy = LocalDate.now(clock);
            if (!"PRODUCTO".equals(tipo)) repository.sumar(empresaId, hoy, tipo);
            else if (productoId != null) repository.sumarProducto(empresaId, hoy, productoId);
        });
    }

    public EstadisticasResponse consultar(long empresaId, int dias) {
        int n = Math.min(Math.max(dias, 7), 90);
        LocalDate hasta = LocalDate.now(clock), desde = hasta.minusDays(n - 1L);
        Map<LocalDate, Map<String, Integer>> mapa = new HashMap<>();
        for (EstadisticaRepository.Fila f : repository.porDia(empresaId, desde, hasta)) {
            mapa.computeIfAbsent(f.fecha(), k -> new HashMap<>()).put(f.tipo(), f.cantidad());
        }
        List<Dia> porDia = new ArrayList<>(n);
        int visitas = 0, pedidos = 0, whatsapp = 0;
        for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
            Map<String, Integer> m = mapa.getOrDefault(d, Map.of());
            Dia dia = new Dia(d, m.getOrDefault("VISITA", 0), m.getOrDefault("PRODUCTO", 0),
                    m.getOrDefault("PEDIDO", 0), m.getOrDefault("WHATSAPP", 0));
            porDia.add(dia);
            visitas += dia.visitas();
            pedidos += dia.pedidos();
            whatsapp += dia.whatsapp();
        }
        return new EstadisticasResponse(n, visitas, pedidos, whatsapp, porDia,
                repository.masVistos(empresaId, desde, hasta, MAS_VISTOS));
    }
}
