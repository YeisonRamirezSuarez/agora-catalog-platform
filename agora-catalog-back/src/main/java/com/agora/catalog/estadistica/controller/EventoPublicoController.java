package com.agora.catalog.estadistica.controller;

import com.agora.catalog.estadistica.dto.EstadisticaDtos.EventoRequest;
import com.agora.catalog.estadistica.service.EstadisticaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * El catálogo público avisa visitas, productos abiertos y clics a WhatsApp. Abierto por diseño
 * (/public/**); el cupo de /public/ en RateLimitFilter limita a quien lo llame en exceso.
 */
@RestController
@RequestMapping("/public/catalogos/{publicHash}/eventos")
public class EventoPublicoController {

    private final EstadisticaService service;

    public EventoPublicoController(EstadisticaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registrar(@PathVariable String publicHash, @Valid @RequestBody EventoRequest r) {
        service.registrar(publicHash, r.tipo(), r.productoId());
    }
}
