package com.agora.catalog.empresa.controller;

import com.agora.catalog.auth.model.UsuarioPrincipal;
import com.agora.catalog.common.exception.ResourceNotFoundException;
import com.agora.catalog.empresa.dto.response.MiEmpresaResponse;
import com.agora.catalog.empresa.repository.EmpresaRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/empresa")
@PreAuthorize("hasRole('ADMIN_EMPRESA')")
public class AdminEmpresaController {

    private final EmpresaRepository empresaRepository;

    public AdminEmpresaController(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    /** La empresa sale del token: no hay forma de pedir otra. */
    @GetMapping
    public MiEmpresaResponse get(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return empresaRepository.findById(principal.empresaId())
                .map(MiEmpresaResponse::of)
                .orElseThrow(() -> new ResourceNotFoundException("La empresa no existe"));
    }
}
