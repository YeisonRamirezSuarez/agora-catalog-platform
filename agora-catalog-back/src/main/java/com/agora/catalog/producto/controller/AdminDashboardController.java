package com.agora.catalog.producto.controller;

import com.agora.catalog.auth.model.UsuarioPrincipal;
import com.agora.catalog.common.exception.ResourceNotFoundException;
import com.agora.catalog.empresa.repository.EmpresaRepository;
import com.agora.catalog.producto.dto.ProductoDtos.DashboardResponse;
import com.agora.catalog.producto.repository.ProductoRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;

/** Inicio del panel: cómo va el catálogo hoy y el link público. */
@RestController
@RequestMapping("/admin/dashboard")
@PreAuthorize("hasRole('ADMIN_EMPRESA')")
public class AdminDashboardController {

    private final ProductoRepository productos;
    private final EmpresaRepository empresas;
    private final Clock clock;

    public AdminDashboardController(ProductoRepository productos, EmpresaRepository empresas, Clock clock) {
        this.productos = productos;
        this.empresas = empresas;
        this.clock = clock;
    }

    @GetMapping
    public DashboardResponse get(@AuthenticationPrincipal UsuarioPrincipal u) {
        var empresa = empresas.findById(u.empresaId()).orElseThrow(() -> new ResourceNotFoundException("La empresa no existe"));
        int[] c = productos.contadores(u.empresaId(), LocalDate.now(clock));
        return new DashboardResponse(c[0], c[1], c[2], c[3], empresa.linkPublico());
    }
}
