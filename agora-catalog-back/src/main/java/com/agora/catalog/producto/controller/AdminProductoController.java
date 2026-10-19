package com.agora.catalog.producto.controller;

import com.agora.catalog.auth.model.UsuarioPrincipal;
import com.agora.catalog.common.dto.PageResponse;
import com.agora.catalog.producto.dto.ProductoDtos.DisponibleRequest;
import com.agora.catalog.producto.dto.ProductoDtos.PreciosRequest;
import com.agora.catalog.producto.dto.ProductoDtos.ProductoRequest;
import com.agora.catalog.producto.dto.ProductoDtos.ProductoResponse;
import com.agora.catalog.producto.repository.ProductoRepository;
import com.agora.catalog.producto.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/admin/productos")
@PreAuthorize("hasRole('ADMIN_EMPRESA')")
public class AdminProductoController {

    private final ProductoService service;

    public AdminProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<ProductoResponse> listar(@AuthenticationPrincipal UsuarioPrincipal u,
                                                 @RequestParam(required = false) String q,
                                                 @RequestParam(required = false) Long categoria,
                                                 @RequestParam(required = false) Boolean disponible,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return service.listar(u.empresaId(), new ProductoRepository.Filtro(q, categoria, disponible), page, size);
    }

    @GetMapping("/{id}")
    public ProductoResponse get(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id) {
        return service.get(u.empresaId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoResponse crear(@AuthenticationPrincipal UsuarioPrincipal u, @Valid @RequestBody ProductoRequest r) {
        return service.crear(u.empresaId(), r);
    }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id,
                                       @Valid @RequestBody ProductoRequest r) {
        return service.actualizar(u.empresaId(), id, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id) {
        service.eliminar(u.empresaId(), id);
    }

    @PatchMapping("/{id}/disponible")
    public ProductoResponse disponible(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id,
                                       @Valid @RequestBody DisponibleRequest r) {
        return service.setDisponible(u.empresaId(), id, r.disponible());
    }

    @PatchMapping("/precios")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void precios(@AuthenticationPrincipal UsuarioPrincipal u, @Valid @RequestBody PreciosRequest r) {
        service.cambiarPrecios(u.empresaId(), r.cambios());
    }

    @PostMapping(value = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductoResponse imagen(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id,
                                   @RequestPart("archivo") MultipartFile archivo) throws IOException {
        return service.subirImagen(u.empresaId(), id, archivo.getBytes());
    }

    @DeleteMapping("/{id}/imagen")
    public ProductoResponse quitarImagen(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id) {
        return service.quitarImagen(u.empresaId(), id);
    }
}
