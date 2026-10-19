package com.agora.catalog.empresa.dto.response;

import com.agora.catalog.empresa.model.Empresa;

/** La empresa del administrador que inició sesión. */
public record MiEmpresaResponse(
        long id,
        String nombre,
        String slug,
        String linkPublico,
        String nombreCorto,
        String lema,
        String colorPrimario,
        String colorSecundario,
        int productos
) {

    public static MiEmpresaResponse of(Empresa empresa) {
        return new MiEmpresaResponse(empresa.id(), empresa.nombre(), empresa.slug(), empresa.linkPublico(),
                empresa.nombreCorto(), empresa.lema(), empresa.colorPrimario(), empresa.colorSecundario(),
                empresa.productos());
    }
}
