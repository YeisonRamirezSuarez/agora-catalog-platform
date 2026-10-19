package com.agora.catalog.usuario.controller;

import com.agora.catalog.auth.model.UsuarioPrincipal;
import com.agora.catalog.common.dto.ActivoRequest;
import com.agora.catalog.usuario.service.SetUsuarioActivoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/superadmin/usuarios")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminUsuarioController {

    private final SetUsuarioActivoService setUsuarioActivoService;

    public SuperAdminUsuarioController(SetUsuarioActivoService setUsuarioActivoService) {
        this.setUsuarioActivoService = setUsuarioActivoService;
    }

    @PatchMapping("/{id}/activo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setActivo(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable long id,
                          @Valid @RequestBody ActivoRequest request) {
        setUsuarioActivoService.setActivo(id, request.activo(), principal.id());
    }
}
