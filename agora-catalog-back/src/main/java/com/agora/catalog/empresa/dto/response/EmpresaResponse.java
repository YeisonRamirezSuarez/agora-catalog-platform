package com.agora.catalog.empresa.dto.response;

import com.agora.catalog.empresa.model.Empresa;
import com.agora.catalog.usuario.model.Usuario;

import java.util.List;

/** Fila del listado del super admin. */
public record EmpresaResponse(
        long id,
        String nombre,
        String slug,
        String publicHash,
        String linkPublico,
        boolean activa,
        int productos,
        List<Administrador> administradores
) {

    public record Administrador(long id, String nombre, String email, boolean activo, boolean debeCambiarPassword) {
    }

    public static EmpresaResponse of(Empresa empresa, List<Usuario> usuarios) {
        return new EmpresaResponse(empresa.id(), empresa.nombre(), empresa.slug(), empresa.publicHash(),
                empresa.linkPublico(), empresa.activa(), empresa.productos(),
                usuarios.stream()
                        .map(u -> new Administrador(u.id(), u.nombre(), u.email(), u.activo(), u.debeCambiarPassword()))
                        .toList());
    }
}
