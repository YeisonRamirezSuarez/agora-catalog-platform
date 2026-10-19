package com.agora.catalog.empresa.controller;

import com.agora.catalog.common.dto.ActivoRequest;
import com.agora.catalog.correo.service.EnviarCorreoService;
import com.agora.catalog.correo.service.PlantillasCorreo;
import com.agora.catalog.empresa.dto.request.AdminEmpresaRequest;
import com.agora.catalog.empresa.dto.request.EmpresaRequest;
import com.agora.catalog.empresa.dto.response.AdminCreadoResponse;
import com.agora.catalog.empresa.dto.response.EmpresaResponse;
import com.agora.catalog.empresa.service.CreateAdminEmpresaService;
import com.agora.catalog.empresa.service.ListEmpresasService;
import com.agora.catalog.empresa.service.SaveEmpresaService;
import com.agora.catalog.empresa.service.SetEmpresaActivaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/superadmin/empresas")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminEmpresaController {

    private final ListEmpresasService listEmpresasService;
    private final SaveEmpresaService saveEmpresaService;
    private final SetEmpresaActivaService setEmpresaActivaService;
    private final CreateAdminEmpresaService createAdminEmpresaService;
    private final EnviarCorreoService enviarCorreoService;
    private final PlantillasCorreo plantillas;

    public SuperAdminEmpresaController(ListEmpresasService listEmpresasService, SaveEmpresaService saveEmpresaService,
                                       SetEmpresaActivaService setEmpresaActivaService,
                                       CreateAdminEmpresaService createAdminEmpresaService,
                                       EnviarCorreoService enviarCorreoService, PlantillasCorreo plantillas) {
        this.listEmpresasService = listEmpresasService;
        this.saveEmpresaService = saveEmpresaService;
        this.setEmpresaActivaService = setEmpresaActivaService;
        this.createAdminEmpresaService = createAdminEmpresaService;
        this.enviarCorreoService = enviarCorreoService;
        this.plantillas = plantillas;
    }

    @GetMapping
    public List<EmpresaResponse> list() {
        return listEmpresasService.list();
    }

    @GetMapping("/{id}")
    public EmpresaResponse get(@PathVariable long id) {
        return listEmpresasService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmpresaResponse create(@Valid @RequestBody EmpresaRequest request) {
        return listEmpresasService.get(saveEmpresaService.create(request.nombre(), request.slug()));
    }

    @PutMapping("/{id}")
    public EmpresaResponse update(@PathVariable long id, @Valid @RequestBody EmpresaRequest request) {
        saveEmpresaService.update(id, request.nombre(), request.slug());
        return listEmpresasService.get(id);
    }

    @PatchMapping("/{id}/activa")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setActiva(@PathVariable long id, @Valid @RequestBody ActivoRequest request) {
        setEmpresaActivaService.setActiva(id, request.activo());
    }

    @PostMapping("/{id}/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminCreadoResponse createAdmin(@PathVariable long id, @Valid @RequestBody AdminEmpresaRequest request) {
        AdminCreadoResponse admin = createAdminEmpresaService.create(id, request.nombre(), request.email());
        if (!Boolean.TRUE.equals(request.enviarCorreo())) {
            return admin;
        }
        // Después de guardar, fuera de la transacción: si el correo falla, el administrador igual
        // queda creado y la pantalla muestra la contraseña para entregarla a mano.
        PlantillasCorreo.Correo correo = plantillas.bienvenida(admin.nombre(),
                listEmpresasService.get(id).nombre(), admin.email(), admin.passwordTemporal());
        EnviarCorreoService.Resultado resultado = enviarCorreoService.enviar(id, "BIENVENIDA", admin.email(),
                correo.asunto(), correo.html());
        return admin.withCorreo(resultado.enviado(), resultado.error());
    }
}
