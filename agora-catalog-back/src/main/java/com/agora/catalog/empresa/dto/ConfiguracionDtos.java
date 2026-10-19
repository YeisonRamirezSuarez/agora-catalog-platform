package com.agora.catalog.empresa.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Configuración del negocio que edita su administrador (GET/PUT /admin/empresa). */
public final class ConfiguracionDtos {

    private ConfiguracionDtos() {
    }

    public record WhatsappDto(
            @NotBlank(message = "Escribe el número de WhatsApp")
            @Pattern(regexp = "^[0-9]{10,15}$", message = "El WhatsApp va con indicativo y solo números (ej. 573163218276)")
            String numero,
            @Size(max = 40, message = "El nombre del número puede tener hasta 40 caracteres")
            String nombre
    ) {
    }

    public record SelloDto(
            @NotBlank(message = "Elige el ícono del sello")
            @Pattern(regexp = "^(CALIDAD|DOMICILIO|WHATSAPP|PRECIO|TRADICION|HORARIO)$", message = "El ícono del sello no es válido")
            String icono,
            @NotBlank(message = "Escribe el texto del sello")
            @Size(max = 48, message = "Cada sello puede tener hasta 48 caracteres")
            String texto
    ) {
    }

    public record ConfiguracionRequest(
            @NotBlank(message = "Escribe el nombre del negocio") @Size(max = 120, message = "El nombre puede tener hasta 120 caracteres") String nombre,
            @NotBlank(message = "Escribe el slug del link") @Size(max = 80, message = "El slug puede tener hasta 80 caracteres")
            @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "El slug solo lleva minúsculas, números y guiones") String slug,
            @Size(max = 24, message = "El nombre corto puede tener hasta 24 caracteres") String nombreCorto,
            @Size(max = 32, message = "El lema puede tener hasta 32 caracteres") String lema,
            @Size(max = 40, message = "El antetítulo de la portada puede tener hasta 40 caracteres") String portadaAntetitulo,
            @Size(max = 40, message = "El titular de la portada puede tener hasta 40 caracteres") String portadaTitulo,
            @Size(max = 40, message = "La línea en cursiva puede tener hasta 40 caracteres") String portadaTituloAcento,
            @Size(max = 140, message = "El texto de la portada puede tener hasta 140 caracteres") String portadaSubtitulo,
            @Size(max = 48, message = "La nota de la portada puede tener hasta 48 caracteres") String portadaNota,
            @Size(max = 30, message = "El texto del sello redondo puede tener hasta 30 caracteres") String portadaSelloTexto,
            @Size(max = 40, message = "El titular del catálogo puede tener hasta 40 caracteres") String catalogoTitulo,
            @Size(max = 30, message = "El final en cursiva puede tener hasta 30 caracteres") String catalogoAcento,
            @Size(max = 140, message = "El texto del catálogo puede tener hasta 140 caracteres") String catalogoSubtitulo,
            @Size(max = 40, message = "La nota de contacto puede tener hasta 40 caracteres") String visitaNota,
            @Pattern(regexp = "^(SALSAMENTARIA|PANADERIA|FRUTERIA|CAFETERIA|MINIMERCADO|FERRETERIA)?$", message = "El dibujo del local no es válido")
            String fachada,
            @NotNull(message = "Indica si van los adornos") Boolean adornos,
            @Size(max = 200, message = "La dirección puede tener hasta 200 caracteres") String direccion,
            @Size(max = 300, message = "El horario puede tener hasta 300 caracteres") String horario,
            @Size(max = 300, message = "La información de domicilios puede tener hasta 300 caracteres") String infoDomicilios,
            @NotNull(message = "Indica si se muestran los precios") Boolean mostrarPrecios,
            @NotNull(message = "Indica si se reciben pedidos por WhatsApp") Boolean pedidosWhatsapp,
            @NotNull(message = "Indica si se muestran los agotados") Boolean mostrarAgotados,
            @NotBlank(message = "Elige el color de botones") @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color de botones no es válido") String colorPrimario,
            @NotBlank(message = "Elige el color de acento") @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color de acento no es válido") String colorSecundario,
            @NotNull(message = "Faltan los números de WhatsApp") @Size(max = 3, message = "Puedes tener hasta 3 números de WhatsApp") List<@Valid WhatsappDto> whatsapps,
            @NotNull(message = "Faltan los sellos") @Size(max = 4, message = "Puedes tener hasta 4 sellos") List<@Valid SelloDto> sellos
    ) {
    }

    /** Rectángulo en píxeles de la foto original. */
    public record Recorte(int x, int y, int ancho, int alto) {
    }

    public record ConfiguracionResponse(
            long id, String nombre, String slug, String publicHash, String linkPublico,
            String nombreCorto, String lema,
            String logoUrl, String portadaUrl,
            // La foto que subió el dueño y el encuadre elegido: para reajustarlo sin volver a subirla.
            String logoOriginalUrl, Recorte logoRecorte, String portadaOriginalUrl, Recorte portadaRecorte,
            String portadaAntetitulo, String portadaTitulo, String portadaTituloAcento, String portadaSubtitulo,
            String portadaNota, String portadaSelloTexto,
            String catalogoTitulo, String catalogoAcento, String catalogoSubtitulo,
            String coleccionesTitulo, String coleccionesTexto, String coleccionesNota,
            String visitaNota, String fachada, boolean adornos,
            String direccion, String horario, String infoDomicilios,
            boolean mostrarPrecios, boolean pedidosWhatsapp, boolean mostrarAgotados,
            String colorPrimario, String colorSecundario,
            List<WhatsappDto> whatsapps, List<SelloDto> sellos
    ) {
    }
}
