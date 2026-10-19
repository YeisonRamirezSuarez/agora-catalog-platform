package com.agora.catalog.catalogopublico.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

/** Todo lo que el catálogo público necesita en una sola respuesta. */
public record CatalogoPublicoResponse(
        Negocio negocio,
        List<Whatsapp> whatsapps,
        List<Sello> sellos,
        List<Categoria> categorias,
        Franja franja,
        List<Coleccion> colecciones,
        List<Producto> productos
) {

    public record Negocio(
            String nombre,
            String nombreCorto,
            String lema,
            String slug,
            String publicHash,
            String logoUrl,
            String portadaUrl,
            String colorPrimario,
            String colorSecundario,
            String portadaAntetitulo,
            String portadaTitulo,
            String portadaTituloAcento,
            String portadaSubtitulo,
            String portadaNota,
            String portadaSelloTexto,
            String catalogoTitulo,
            String catalogoAcento,
            String catalogoSubtitulo,
            String coleccionesTitulo,
            String coleccionesTexto,
            String coleccionesNota,
            String visitaNota,
            String fachada,
            boolean adornos,
            String direccion,
            String horario,
            String infoDomicilios,
            boolean mostrarPrecios,
            boolean pedidosWhatsapp
    ) {
    }

    public record Whatsapp(String numero, String nombre) {
    }

    public record Sello(String icono, String texto) {
    }

    public record Categoria(long id, String nombre, String imagenUrl) {
    }

    /** La categoría destacada con su foto grande en el inicio. */
    public record Franja(long categoriaId, String titulo, String frase, String nota, String imagenUrl) {
    }

    public record Coleccion(long id, String nombre, String descripcion, String imagenUrl, List<Long> productos) {
    }

    /**
     * Con precios ocultos, los cuatro campos de precio no se envían (null y fuera del JSON): ocultarlos
     * solo en el front dejaría el precio a la vista de cualquiera que mire la respuesta.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Producto(
            long id,
            long categoriaId,
            String nombre,
            String descripcion,
            String presentacion,
            String unidadVenta,
            BigDecimal precio,
            BigDecimal precioOferta,
            BigDecimal precioFinal,
            Boolean ofertaVigente,
            Integer descuento,
            String imagenUrl,
            String imagenThumbUrl,
            boolean disponible,
            boolean destacado,
            boolean nuevo
    ) {
    }
}
