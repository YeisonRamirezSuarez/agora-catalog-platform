package com.agora.catalog.pedidotienda.controller;

import com.agora.catalog.auth.model.UsuarioPrincipal;
import com.agora.catalog.pedidotienda.dto.PedidoTiendaDtos.EditarPedidoRequest;
import com.agora.catalog.pedidotienda.dto.PedidoTiendaDtos.PedidoRequest;
import com.agora.catalog.pedidotienda.dto.PedidoTiendaDtos.PedidoResponse;
import com.agora.catalog.pedidotienda.service.CrearPedidoTiendaService;
import com.agora.catalog.pedidotienda.service.EditarPedidoTiendaService;
import jakarta.validation.Valid;
import com.agora.catalog.modulo.model.Modulo;
import com.agora.catalog.modulo.web.RequiereModulo;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/pedidos-tienda")
@PreAuthorize("hasRole('ADMIN_EMPRESA')")
@RequiereModulo(Modulo.VENTA_RUTA)
public class AdminPedidoTiendaController {

    private final CrearPedidoTiendaService crear;
    private final EditarPedidoTiendaService editar;

    public AdminPedidoTiendaController(CrearPedidoTiendaService crear, EditarPedidoTiendaService editar) {
        this.crear = crear;
        this.editar = editar;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse crear(@AuthenticationPrincipal UsuarioPrincipal u, @Valid @RequestBody PedidoRequest r) {
        return crear.crear(u.empresaId(), u.id(), r);
    }

    @PutMapping("/{id}")
    public PedidoResponse editar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id,
                                 @Valid @RequestBody EditarPedidoRequest r) {
        return editar.editar(u.empresaId(), u.id(), id, r);
    }

    /** Nombres de tienda ya usados en pedidos que contienen {@code q}, para sugerirlos al escribir. */
    @GetMapping("/tiendas")
    public List<String> tiendas(@AuthenticationPrincipal UsuarioPrincipal u, @RequestParam(defaultValue = "") String q) {
        return crear.nombresTiendas(u.empresaId(), q);
    }

    @GetMapping("/{id}")
    public PedidoResponse get(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id) {
        return crear.get(u.empresaId(), id);
    }
}
