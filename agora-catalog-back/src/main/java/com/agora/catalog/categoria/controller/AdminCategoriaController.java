package com.agora.catalog.categoria.controller;

import com.agora.catalog.auth.model.UsuarioPrincipal;
import com.agora.catalog.categoria.dto.CategoriaDtos.CategoriaRequest;
import com.agora.catalog.categoria.dto.CategoriaDtos.CategoriaResponse;
import com.agora.catalog.categoria.dto.CategoriaDtos.FranjaRequest;
import com.agora.catalog.categoria.dto.CategoriaDtos.OrdenRequest;
import com.agora.catalog.categoria.service.CategoriaService;
import com.agora.catalog.storage.ImagenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/admin/categorias")
@PreAuthorize("hasRole('ADMIN_EMPRESA')")
public class AdminCategoriaController {

    private final CategoriaService service;

    public AdminCategoriaController(CategoriaService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoriaResponse> list(@AuthenticationPrincipal UsuarioPrincipal u) {
        return service.list(u.empresaId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaResponse crear(@AuthenticationPrincipal UsuarioPrincipal u, @Valid @RequestBody CategoriaRequest r) {
        return service.crear(u.empresaId(), r.nombre(), r.activa());
    }

    @PutMapping("/{id}")
    public CategoriaResponse actualizar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id,
                                        @Valid @RequestBody CategoriaRequest r) {
        return service.actualizar(u.empresaId(), id, r.nombre(), r.activa());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id) {
        service.eliminar(u.empresaId(), id);
    }

    @PutMapping("/orden")
    public List<CategoriaResponse> ordenar(@AuthenticationPrincipal UsuarioPrincipal u, @Valid @RequestBody OrdenRequest r) {
        return service.ordenar(u.empresaId(), r.ids());
    }

    /** Multipart: titulo, frase, nota, archivo (opcional si ya tiene foto) y el recorte en píxeles (opcional). */
    @PutMapping(value = "/{id}/franja", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CategoriaResponse franja(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id,
                                    @Valid @ModelAttribute FranjaRequest r,
                                    @RequestParam(required = false) MultipartFile archivo,
                                    @RequestParam(required = false) Integer x, @RequestParam(required = false) Integer y,
                                    @RequestParam(required = false) Integer ancho, @RequestParam(required = false) Integer alto)
            throws IOException {
        ImagenService.Recorte recorte = x != null && y != null && ancho != null && alto != null
                ? new ImagenService.Recorte(x, y, ancho, alto) : null;
        return service.guardarFranja(u.empresaId(), id, r, archivo == null ? null : archivo.getBytes(), recorte);
    }

    @DeleteMapping("/{id}/franja")
    public CategoriaResponse quitarFranja(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id) {
        return service.quitarFranja(u.empresaId(), id);
    }
}
