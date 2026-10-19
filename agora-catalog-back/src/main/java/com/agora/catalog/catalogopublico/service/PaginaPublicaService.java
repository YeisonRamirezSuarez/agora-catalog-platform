package com.agora.catalog.catalogopublico.service;

import com.agora.catalog.catalogopublico.dto.response.CatalogoPublicoResponse.Negocio;
import com.agora.catalog.catalogopublico.repository.CatalogoPublicoRepository;
import com.agora.catalog.storage.ImagenService;
import com.agora.catalog.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * La página {@code /c/{slug}-{hash}} (ESPECIFICACION.md §5). WhatsApp y las redes no ejecutan
 * JavaScript: para mostrar la vista previa del link necesitan las etiquetas Open Graph en el HTML.
 * Se devuelve el {@code index.html} del front con esas etiquetas escritas en el marcador
 * {@code <!--og-->}; Angular arranca igual en el navegador.
 */
@Slf4j
@Service
public class PaginaPublicaService {

    /** {@code status} 301 lleva {@code location}; el resto lleva {@code html}. */
    public record Pagina(int status, String location, String html) {
    }

    private static final Pattern HASH = Pattern.compile("[a-z0-9]{1,32}");
    private static final String MARCADOR = "<!--og-->";
    private static final String TITULO = "<title>Agorá Catalog</title>";
    private static final String DESCRIPCION = "Mira el catálogo y haz tu pedido por WhatsApp.";

    private final CatalogoPublicoRepository repository;
    private final StorageService storage;
    private final ImagenService imagenes;
    private final String baseUrl;
    private final String indexPath;

    public PaginaPublicaService(CatalogoPublicoRepository repository, StorageService storage, ImagenService imagenes,
                                @Value("${settings.frontend-url}") String baseUrl,
                                @Value("${settings.front-index-path:}") String indexPath) {
        this.repository = repository;
        this.storage = storage;
        this.imagenes = imagenes;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.indexPath = indexPath;
    }

    /**
     * @param slugHash lo que va después de {@code /c/}; la empresa se busca solo por el hash (lo que
     *                 sigue al último guion), así un link con el slug viejo sigue sirviendo
     * @param resto    subpágina ({@code /catalogo}) y query string, que se conservan al redirigir
     */
    public Pagina pagina(String slugHash, String resto) {
        Optional<String> index = index();
        if (index.isEmpty()) {
            return new Pagina(503, null, "<!doctype html><meta charset=\"utf-8\"><p>El catálogo no está disponible en este momento.</p>");
        }
        String hash = slugHash.substring(slugHash.lastIndexOf('-') + 1);
        Optional<Negocio> negocio = HASH.matcher(hash).matches()
                ? repository.findEmpresaActiva(hash).map(CatalogoPublicoRepository.EmpresaPublica::negocio)
                : Optional.empty();
        // Sin negocio, Angular pide el catálogo, recibe 404 y muestra "Catálogo no disponible".
        if (negocio.isEmpty()) return new Pagina(404, null, index.get());

        Negocio n = negocio.get();
        String actual = n.slug() + "-" + n.publicHash();
        if (!actual.equals(slugHash)) return new Pagina(301, "/c/" + actual + resto, null);
        return new Pagina(200, null, conEtiquetas(index.get(), n, baseUrl + "/c/" + actual));
    }

    private String conEtiquetas(String index, Negocio n, String url) {
        String descripcion = primero(n.portadaSubtitulo(), n.lema(), DESCRIPCION);
        String imagen = imagen(n);
        StringBuilder og = new StringBuilder()
                .append(meta("og:type", "website"))
                .append(meta("og:site_name", "Agorá Catalog"))
                .append(meta("og:locale", "es_CO"))
                .append(meta("og:title", n.nombre()))
                .append(meta("og:description", descripcion))
                .append(meta("og:url", url));
        if (imagen != null) {
            og.append(meta("og:image", baseUrl + imagen)).append("<meta name=\"twitter:card\" content=\"summary_large_image\">\n");
        }
        og.append("<meta name=\"description\" content=\"").append(esc(descripcion)).append("\">\n");
        return index.replace(TITULO, "<title>" + esc(n.nombre()) + "</title>")
                .replace(MARCADOR, og.toString());
    }

    /** La portada en versión liviana (se genera la primera vez); sin portada, el logo, que ya es chico. */
    private String imagen(Negocio n) {
        Path portada = storage.ruta(n.portadaUrl());
        if (portada == null || !Files.isRegularFile(portada)) return n.logoUrl();
        String url = StorageService.urlVistaPrevia(n.portadaUrl());
        Path destino = storage.ruta(url);
        if (Files.isRegularFile(destino)) return url;
        try {
            Path tmp = Files.createTempFile(destino.getParent(), "og", ".tmp");
            Files.write(tmp, imagenes.vistaPrevia(Files.readAllBytes(portada)));
            Files.move(tmp, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return url;
        } catch (IOException | RuntimeException e) {
            log.warn("[OG] No se pudo generar la vista previa de {}: {}", portada, e.getMessage());
            return n.logoUrl();
        }
    }

    /** Se lee en cada visita: es pequeño y así un build nuevo del front se ve sin reiniciar la API. */
    private Optional<String> index() {
        if (indexPath.isBlank()) {
            log.error("[OG] Falta FRONT_INDEX_PATH: /c/ no puede devolver la página del catálogo");
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(Path.of(indexPath)));
        } catch (IOException e) {
            log.error("[OG] No se pudo leer FRONT_INDEX_PATH={}: {}", indexPath, e.getMessage());
            return Optional.empty();
        }
    }

    private static String meta(String propiedad, String valor) {
        return "<meta property=\"" + propiedad + "\" content=\"" + esc(valor) + "\">\n";
    }

    /** Solo los caracteres especiales de HTML: las tildes van tal cual (la página es UTF-8). */
    private static String esc(String s) {
        return HtmlUtils.htmlEscape(s, "UTF-8");
    }

    private static String primero(String... valores) {
        for (String v : valores) if (v != null && !v.isBlank()) return v.replace('\n', ' ').trim();
        return "";
    }
}
