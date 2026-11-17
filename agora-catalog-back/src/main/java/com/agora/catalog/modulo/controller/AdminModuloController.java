package com.agora.catalog.modulo.controller;

import com.agora.catalog.auth.model.UsuarioPrincipal;
import com.agora.catalog.modulo.model.Modulo;
import com.agora.catalog.modulo.repository.ModuloRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/modulos")
@PreAuthorize("hasRole('ADMIN_EMPRESA')")
public class AdminModuloController {

    private final ModuloRepository modulos;

    public AdminModuloController(ModuloRepository modulos) {
        this.modulos = modulos;
    }

    /** Los módulos encendidos de la empresa del token, para armar el menú del panel. */
    @GetMapping
    public List<String> activos(@AuthenticationPrincipal UsuarioPrincipal u) {
        return modulos.activos(u.empresaId()).stream().map(Modulo::name).toList();
    }
}
