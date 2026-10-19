package com.agora.catalog.usuario.controller;

import com.agora.catalog.auth.model.UsuarioPrincipal;
import com.agora.catalog.common.dto.ActivoRequest;
import com.agora.catalog.correo.service.EnviarCorreoService;
import com.agora.catalog.correo.service.PlantillasCorreo;
import com.agora.catalog.empresa.dto.response.AdminCreadoResponse;
import com.agora.catalog.usuario.service.RestablecerPasswordService;
import com.agora.catalog.usuario.service.SetUsuarioActivoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/superadmin/usuarios")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminUsuarioController {

    /** {@code enviarCorreo}: mandarle la contraseña nueva con el proveedor de correo configurado. */
    public record RestablecerRequest(Boolean enviarCorreo) {
    }

    private final SetUsuarioActivoService setUsuarioActivoService;
    private final RestablecerPasswordService restablecerPasswordService;
    private final EnviarCorreoService enviarCorreoService;
    private final PlantillasCorreo plantillas;

    public SuperAdminUsuarioController(SetUsuarioActivoService setUsuarioActivoService,
                                       RestablecerPasswordService restablecerPasswordService,
                                       EnviarCorreoService enviarCorreoService, PlantillasCorreo plantillas) {
        this.setUsuarioActivoService = setUsuarioActivoService;
        this.restablecerPasswordService = restablecerPasswordService;
        this.enviarCorreoService = enviarCorreoService;
        this.plantillas = plantillas;
    }

    @PatchMapping("/{id}/activo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setActivo(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable long id,
                          @Valid @RequestBody ActivoRequest request) {
        setUsuarioActivoService.setActivo(id, request.activo(), principal.id());
    }

    /** Contraseña temporal nueva para un administrador que olvidó la suya; se muestra una sola vez. */
    @PostMapping("/{id}/password")
    public AdminCreadoResponse restablecer(@PathVariable long id, @RequestBody(required = false) RestablecerRequest request) {
        RestablecerPasswordService.Restablecida r = restablecerPasswordService.restablecer(id);
        if (request == null || !Boolean.TRUE.equals(request.enviarCorreo())) {
            return r.admin();
        }
        // Fuera de la transacción, como al crear el administrador: si el correo falla, la contraseña
        // igual quedó cambiada y la pantalla la muestra para entregarla a mano.
        PlantillasCorreo.Correo correo = plantillas.bienvenida(r.admin().nombre(), r.empresaNombre(), r.admin().email(),
                r.admin().passwordTemporal());
        EnviarCorreoService.Resultado resultado = enviarCorreoService.enviar(r.empresaId(), "CONTRASENA", r.admin().email(),
                correo.asunto(), correo.html());
        return r.admin().withCorreo(resultado.enviado(), resultado.error());
    }
}
