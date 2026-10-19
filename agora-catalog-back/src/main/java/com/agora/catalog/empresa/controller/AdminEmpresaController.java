package com.agora.catalog.empresa.controller;

import com.agora.catalog.auth.model.UsuarioPrincipal;
import com.agora.catalog.empresa.dto.ConfiguracionDtos.ConfiguracionRequest;
import com.agora.catalog.empresa.dto.ConfiguracionDtos.ConfiguracionResponse;
import com.agora.catalog.empresa.dto.ConfiguracionDtos.Recorte;
import com.agora.catalog.empresa.service.ConfiguracionService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** Configuración del negocio. La empresa sale del token: no hay forma de pedir ni cambiar otra. */
@RestController
@RequestMapping("/admin/empresa")
@PreAuthorize("hasRole('ADMIN_EMPRESA')")
public class AdminEmpresaController {

    private final ConfiguracionService service;

    public AdminEmpresaController(ConfiguracionService service) {
        this.service = service;
    }

    @GetMapping
    public ConfiguracionResponse get(@AuthenticationPrincipal UsuarioPrincipal u) {
        return service.get(u.empresaId());
    }

    @PutMapping
    public ConfiguracionResponse guardar(@AuthenticationPrincipal UsuarioPrincipal u, @Valid @RequestBody ConfiguracionRequest r) {
        return service.guardar(u.empresaId(), r);
    }

    /**
     * Logo o portada: multipart con {@code archivo} (opcional: sin él se recorta de nuevo el original
     * guardado) y el rectángulo del recorte en píxeles del original ({@code x}, {@code y}, {@code ancho}, {@code alto}).
     */
    @PostMapping(value = "/{tipo:logo|portada}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ConfiguracionResponse foto(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable String tipo,
                                      @RequestParam(required = false) MultipartFile archivo,
                                      @RequestParam(required = false) Integer x, @RequestParam(required = false) Integer y,
                                      @RequestParam(required = false) Integer ancho, @RequestParam(required = false) Integer alto)
            throws IOException {
        Recorte recorte = x != null && y != null && ancho != null && alto != null ? new Recorte(x, y, ancho, alto) : null;
        return service.guardarFoto(u.empresaId(), tipo, archivo == null ? null : archivo.getBytes(), recorte);
    }

    @DeleteMapping("/{tipo:logo|portada}")
    public ConfiguracionResponse quitarFoto(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable String tipo) {
        return service.quitarFoto(u.empresaId(), tipo);
    }
}
