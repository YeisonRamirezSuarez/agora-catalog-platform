package com.agora.catalog.correo.dto.request;

import com.agora.catalog.correo.model.ProveedorCorreo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** @param password vacía para conservar la que ya está guardada */
public record ProveedorCorreoRequest(
        @NotBlank(message = "Escribe el servidor SMTP (ej. smtp.gmail.com)")
        @Size(max = 160, message = "El servidor puede tener hasta 160 caracteres")
        String host,

        @NotNull(message = "Escribe el puerto (587 o 465)")
        @Min(value = 1, message = "El puerto no es válido")
        @Max(value = 65535, message = "El puerto no es válido")
        Integer puerto,

        @Size(max = 160, message = "El usuario puede tener hasta 160 caracteres")
        String usuario,

        @Size(max = 200, message = "La contraseña puede tener hasta 200 caracteres")
        String password,

        @NotBlank(message = "Escribe el correo remitente")
        @Email(message = "El correo remitente no es válido")
        @Size(max = 160, message = "El correo remitente puede tener hasta 160 caracteres")
        String remitenteEmail,

        @NotBlank(message = "Escribe el nombre del remitente")
        @Size(max = 120, message = "El nombre del remitente puede tener hasta 120 caracteres")
        String remitenteNombre,

        @NotNull(message = "Elige la seguridad de la conexión")
        ProveedorCorreo.Seguridad seguridad,

        @NotNull(message = "Indica si el proveedor queda activo")
        Boolean activo
) {
}
