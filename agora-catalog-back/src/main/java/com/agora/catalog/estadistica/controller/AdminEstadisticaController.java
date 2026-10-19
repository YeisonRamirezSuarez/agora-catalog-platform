package com.agora.catalog.estadistica.controller;

import com.agora.catalog.auth.model.UsuarioPrincipal;
import com.agora.catalog.estadistica.dto.EstadisticaDtos.EstadisticasResponse;
import com.agora.catalog.estadistica.service.EstadisticaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/estadisticas")
@PreAuthorize("hasRole('ADMIN_EMPRESA')")
public class AdminEstadisticaController {

    private final EstadisticaService service;

    public AdminEstadisticaController(EstadisticaService service) {
        this.service = service;
    }

    /** Últimos {@code dias} días, hoy incluido (entre 7 y 90). */
    @GetMapping
    public EstadisticasResponse get(@AuthenticationPrincipal UsuarioPrincipal u, @RequestParam(defaultValue = "30") int dias) {
        return service.consultar(u.empresaId(), dias);
    }
}
