package com.agora.catalog.carro.controller;

import com.agora.catalog.auth.model.UsuarioPrincipal;
import com.agora.catalog.carro.dto.CarroDtos.AjusteRequest;
import com.agora.catalog.carro.dto.CarroDtos.CargaRequest;
import com.agora.catalog.carro.dto.CarroDtos.CarroDetalleResponse;
import com.agora.catalog.carro.dto.CarroDtos.CarroRequest;
import com.agora.catalog.carro.dto.CarroDtos.CarroResponse;
import com.agora.catalog.carro.dto.CarroDtos.DescargueRequest;
import com.agora.catalog.carro.dto.CarroDtos.MovimientoResponse;
import com.agora.catalog.carro.dto.CarroDtos.NuevaRutaRequest;
import com.agora.catalog.carro.dto.CarroDtos.RutaResponse;
import com.agora.catalog.carro.service.CarroService;
import com.agora.catalog.carro.service.RutaCarroService;
import com.agora.catalog.carro.service.StockCarroService;
import com.agora.catalog.common.dto.ActivoRequest;
import com.agora.catalog.common.dto.PageResponse;
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

import java.util.List;

@RestController
@RequestMapping("/admin/carros")
@PreAuthorize("hasRole('ADMIN_EMPRESA')")
@RequiereModulo(Modulo.VENTA_RUTA)
public class AdminCarroController {

    private final CarroService service;
    private final StockCarroService stock;
    private final RutaCarroService rutas;

    public AdminCarroController(CarroService service, StockCarroService stock, RutaCarroService rutas) {
        this.service = service;
        this.stock = stock;
        this.rutas = rutas;
    }

    @GetMapping
    public List<CarroResponse> list(@AuthenticationPrincipal UsuarioPrincipal u) {
        return service.list(u.empresaId());
    }

    @GetMapping("/{id}")
    public CarroDetalleResponse detalle(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id) {
        return service.detalle(u.empresaId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarroDetalleResponse crear(@AuthenticationPrincipal UsuarioPrincipal u, @Valid @RequestBody CarroRequest r) {
        return service.crear(u.empresaId(), r);
    }

    @PutMapping("/{id}")
    public CarroDetalleResponse actualizar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id,
                                           @Valid @RequestBody CarroRequest r) {
        return service.actualizar(u.empresaId(), id, r);
    }

    @PatchMapping("/{id}/activo")
    public CarroResponse setActivo(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id,
                                   @Valid @RequestBody ActivoRequest r) {
        return service.setActivo(u.empresaId(), id, r.activo());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id) {
        service.eliminar(u.empresaId(), id);
    }

    @PostMapping("/{id}/cargas")
    public CarroDetalleResponse cargar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id,
                                       @Valid @RequestBody CargaRequest r) {
        return stock.cargar(u.empresaId(), u.id(), id, r);
    }

    @PostMapping("/{id}/ajustes")
    public CarroDetalleResponse ajustar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id,
                                        @Valid @RequestBody AjusteRequest r) {
        return stock.ajustar(u.empresaId(), u.id(), id, r);
    }

    @PostMapping("/{id}/descargue")
    public CarroDetalleResponse descargar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id,
                                          @Valid @RequestBody DescargueRequest r) {
        return stock.descargar(u.empresaId(), u.id(), id, r.nota());
    }

    @GetMapping("/{id}/movimientos")
    public PageResponse<MovimientoResponse> movimientos(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return stock.movimientos(u.empresaId(), id, page, size);
    }

    /** Empieza una ruta nueva: cierra la que iba y decide si lo que sobró sigue en el carro. */
    @PostMapping("/{id}/rutas")
    public CarroDetalleResponse nuevaRuta(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id,
                                          @Valid @RequestBody NuevaRutaRequest r) {
        return rutas.nueva(u.empresaId(), u.id(), id, r.llevarSobrante());
    }

    @GetMapping("/{id}/rutas")
    public PageResponse<RutaResponse> rutas(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return rutas.rutas(u.empresaId(), id, page, size);
    }
}
