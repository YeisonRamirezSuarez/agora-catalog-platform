package com.agora.catalog.correo.controller;

import com.agora.catalog.correo.dto.request.ProveedorCorreoRequest;
import com.agora.catalog.correo.dto.request.PruebaCorreoRequest;
import com.agora.catalog.correo.dto.response.ProveedorCorreoResponse;
import com.agora.catalog.correo.dto.response.CorreoEnvioResponse;
import com.agora.catalog.correo.repository.CorreoEnvioRepository;
import com.agora.catalog.correo.repository.ProveedorCorreoRepository;
import com.agora.catalog.correo.service.EnviarCorreoService;
import com.agora.catalog.correo.service.GuardarProveedorCorreoService;
import com.agora.catalog.correo.service.PlantillasCorreo;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/superadmin/correo")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminCorreoController {

    private final ProveedorCorreoRepository proveedorRepository;
    private final CorreoEnvioRepository envioRepository;
    private final GuardarProveedorCorreoService guardarService;
    private final EnviarCorreoService enviarService;
    private final PlantillasCorreo plantillas;

    public SuperAdminCorreoController(ProveedorCorreoRepository proveedorRepository,
                                      CorreoEnvioRepository envioRepository,
                                      GuardarProveedorCorreoService guardarService,
                                      EnviarCorreoService enviarService, PlantillasCorreo plantillas) {
        this.proveedorRepository = proveedorRepository;
        this.envioRepository = envioRepository;
        this.guardarService = guardarService;
        this.enviarService = enviarService;
        this.plantillas = plantillas;
    }

    /** 204 mientras no se haya configurado ninguno. */
    @GetMapping
    public ResponseEntity<ProveedorCorreoResponse> get() {
        return proveedorRepository.find()
                .map(p -> ResponseEntity.ok(ProveedorCorreoResponse.of(p)))
                .orElse(ResponseEntity.noContent().build());
    }

    @PutMapping
    public ProveedorCorreoResponse save(@Valid @RequestBody ProveedorCorreoRequest request) {
        return ProveedorCorreoResponse.of(guardarService.save(request));
    }

    @PostMapping("/prueba")
    public EnviarCorreoService.Resultado probar(@Valid @RequestBody PruebaCorreoRequest request) {
        PlantillasCorreo.Correo correo = plantillas.prueba();
        return enviarService.enviar(null, "PRUEBA", request.destinatario().trim(), correo.asunto(), correo.html());
    }

    // shortcut: los últimos 50 sin filtros; paginar con PageResponse cuando se usen campañas.
    @GetMapping("/envios")
    public List<CorreoEnvioResponse> envios() {
        return envioRepository.findRecientes(50);
    }
}
