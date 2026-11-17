package com.agora.catalog.tienda.controller;

import com.agora.catalog.auth.model.UsuarioPrincipal;
import com.agora.catalog.common.dto.ActivoRequest;
import com.agora.catalog.common.dto.PageResponse;
import com.agora.catalog.tienda.dto.TiendaDtos.TiendaRequest;
import com.agora.catalog.tienda.dto.TiendaDtos.TiendaResponse;
import com.agora.catalog.tienda.service.TiendaService;
import jakarta.validation.Valid;
import com.agora.catalog.modulo.model.Modulo;
import com.agora.catalog.modulo.web.RequiereModulo;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/tiendas")
@PreAuthorize("hasRole('ADMIN_EMPRESA')")
@RequiereModulo(Modulo.VENTA_RUTA)
public class AdminTiendaController {

    private final TiendaService service;

    public AdminTiendaController(TiendaService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<TiendaResponse> listar(@AuthenticationPrincipal UsuarioPrincipal u,
                                               @RequestParam(required = false) String q,
                                               @RequestParam(required = false) Boolean activa,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return service.listar(u.empresaId(), q, activa, page, size);
    }

    @GetMapping("/{id}")
    public TiendaResponse get(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id) {
        return service.get(u.empresaId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TiendaResponse crear(@AuthenticationPrincipal UsuarioPrincipal u, @Valid @RequestBody TiendaRequest r) {
        return service.crear(u.empresaId(), r);
    }

    @PutMapping("/{id}")
    public TiendaResponse actualizar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id,
                                     @Valid @RequestBody TiendaRequest r) {
        return service.actualizar(u.empresaId(), id, r);
    }

    @PatchMapping("/{id}/activa")
    public TiendaResponse setActiva(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id,
                                    @Valid @RequestBody ActivoRequest r) {
        return service.setActiva(u.empresaId(), id, r.activo());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id) {
        service.eliminar(u.empresaId(), id);
    }
}
