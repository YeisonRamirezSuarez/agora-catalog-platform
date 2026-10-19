package com.agora.catalog.empresa.model;

/** Datos de la empresa que usan el panel y el super admin. La configuración del catálogo llega en la fase 5. */
public record Empresa(
        long id,
        String nombre,
        String slug,
        String publicHash,
        String nombreCorto,
        String lema,
        String colorPrimario,
        String colorSecundario,
        boolean activa,
        int productos
) {

    /** Ruta del link público: el backend la resuelve por el hash, el slug es solo legible. */
    public String linkPublico() {
        return "/c/" + slug + "-" + publicHash;
    }
}
