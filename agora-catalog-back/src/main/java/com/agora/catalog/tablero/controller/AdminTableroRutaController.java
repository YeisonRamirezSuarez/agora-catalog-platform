package com.agora.catalog.tablero.controller;

import com.agora.catalog.auth.model.UsuarioPrincipal;
import com.agora.catalog.modulo.model.Modulo;
import com.agora.catalog.modulo.web.RequiereModulo;
import com.agora.catalog.tablero.dto.TableroDtos.TableroResponse;
import com.agora.catalog.tablero.service.TableroRutaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/tablero-ruta")
@PreAuthorize("hasRole('ADMIN_EMPRESA')")
@RequiereModulo(Modulo.VENTA_RUTA)
public class AdminTableroRutaController {

    private final TableroRutaService service;

    public AdminTableroRutaController(TableroRutaService service) {
        this.service = service;
    }

    /** El tablero de un día ({@code fecha=2026-11-22}); sin fecha, el de hoy. */
    @GetMapping
    public TableroResponse dia(@AuthenticationPrincipal UsuarioPrincipal u,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return service.dia(u.empresaId(), fecha);
    }
}
