package com.agora.catalog.coleccion.controller;

import com.agora.catalog.coleccion.service.ColeccionService;
import com.agora.catalog.auth.model.UsuarioPrincipal;
import com.agora.catalog.coleccion.dto.ColeccionDtos.ColeccionRequest;
import com.agora.catalog.coleccion.dto.ColeccionDtos.ColeccionResponse;
import com.agora.catalog.coleccion.dto.ColeccionDtos.OrdenRequest;
import com.agora.catalog.coleccion.dto.ColeccionDtos.SeccionRequest;
import com.agora.catalog.empresa.dto.ConfiguracionDtos.ConfiguracionResponse;
import com.agora.catalog.empresa.service.ConfiguracionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/admin/colecciones")
@PreAuthorize("hasRole('ADMIN_EMPRESA')")
public class AdminColeccionController {

    private final ColeccionService service;
    private final ConfiguracionService configuracion;

    public AdminColeccionController(ColeccionService service, ConfiguracionService configuracion) {
        this.service = service;
        this.configuracion = configuracion;
    }

    @GetMapping
    public List<ColeccionResponse> list(@AuthenticationPrincipal UsuarioPrincipal u) {
        return service.list(u.empresaId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ColeccionResponse crear(@AuthenticationPrincipal UsuarioPrincipal u, @Valid @RequestBody ColeccionRequest r) {
        return service.crear(u.empresaId(), r);
    }

    @PutMapping("/{id}")
    public ColeccionResponse actualizar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id,
                                        @Valid @RequestBody ColeccionRequest r) {
        return service.actualizar(u.empresaId(), id, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id) {
        service.eliminar(u.empresaId(), id);
    }

    @PutMapping("/orden")
    public List<ColeccionResponse> ordenar(@AuthenticationPrincipal UsuarioPrincipal u, @Valid @RequestBody OrdenRequest r) {
        return service.ordenar(u.empresaId(), r.ids());
    }

    @PostMapping(value = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ColeccionResponse imagen(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable long id,
                                    @RequestPart("archivo") MultipartFile archivo) throws IOException {
        return service.subirImagen(u.empresaId(), id, archivo.getBytes());
    }

    /** Título, texto y nota de la sección «¿Cuál es el plan?» del inicio. */
    @PutMapping("/seccion")
    public ConfiguracionResponse seccion(@AuthenticationPrincipal UsuarioPrincipal u, @Valid @RequestBody SeccionRequest r) {
        return configuracion.guardarColecciones(u.empresaId(), r.titulo(), r.texto(), r.nota());
    }
}
