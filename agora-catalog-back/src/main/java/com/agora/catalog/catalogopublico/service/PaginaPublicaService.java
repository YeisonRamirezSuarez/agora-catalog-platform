package com.agora.catalog.catalogopublico.service;

import com.agora.catalog.catalogopublico.dto.response.CatalogoPublicoResponse.Negocio;
import com.agora.catalog.catalogopublico.repository.CatalogoPublicoRepository;
import com.agora.catalog.catalogopublico.repository.CatalogoPublicoRepository.EmpresaPublica;
import com.agora.catalog.catalogopublico.repository.CatalogoPublicoRepository.ProductoFila;
import com.agora.catalog.producto.service.ReglasProducto;
import com.agora.catalog.storage.ImagenService;
import com.agora.catalog.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    /** Imagen de Agorá Catalog (public/ del front) para el negocio sin portada ni logo. */
    private static final String IMAGEN_POR_DEFECTO = "/og-default.jpg";

    private final CatalogoPublicoRepository repository;
    private final StorageService storage;
    private final ImagenService imagenes;
    private final Clock clock;
    private final String baseUrl;
    private final String indexPath;

    public PaginaPublicaService(CatalogoPublicoRepository repository, StorageService storage, ImagenService imagenes,
                                Clock clock, @Value("${settings.frontend-url}") String baseUrl,
                                @Value("${settings.front-index-path:}") String indexPath) {
        this.repository = repository;
        this.storage = storage;
        this.imagenes = imagenes;
        this.clock = clock;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.indexPath = indexPath;
    }

    /**
     * @param slugHash lo que va después de {@code /c/}; la empresa se busca solo por el hash (lo que
     *                 sigue al último guion), así un link con el slug viejo sigue sirviendo
     * @param resto    subpágina ({@code /catalogo}) y query string, que se conservan al redirigir
     * @param productoId el {@code ?p=} de un producto compartido: la vista previa es la de ese producto
     */
    public Pagina pagina(String slugHash, String resto, Long productoId) {
        Optional<String> index = index();
        if (index.isEmpty()) {
            return new Pagina(503, null, "<!doctype html><meta charset=\"utf-8\"><p>El catálogo no está disponible en este momento.</p>");
        }
        String hash = slugHash.substring(slugHash.lastIndexOf('-') + 1);
        Optional<EmpresaPublica> empresa = HASH.matcher(hash).matches() ? repository.findEmpresaActiva(hash) : Optional.empty();
        // Sin negocio, Angular pide el catálogo, recibe 404 y muestra "Catálogo no disponible".
        if (empresa.isEmpty()) return new Pagina(404, null, index.get());

        Negocio n = empresa.get().negocio();
        String actual = n.slug() + "-" + n.publicHash();
        if (!actual.equals(slugHash)) return new Pagina(301, "/c/" + actual + resto, null);
        // Un producto que no existe, es de otro negocio o está oculto: la vista previa es la del negocio.
        Optional<ProductoFila> producto = productoId == null ? Optional.empty()
                : repository.findProducto(empresa.get().id(), productoId, empresa.get().mostrarAgotados());
        if (producto.isPresent()) {
            ProductoFila p = producto.get();
            return new Pagina(200, null, conEtiquetas(index.get(), n, p.nombre() + " · " + n.nombre(), descripcion(n, p),
                    p.imagenUrl() != null ? p.imagenUrl() : imagen(n), baseUrl + "/c/" + actual + "/catalogo?p=" + p.id()));
        }
        return new Pagina(200, null, conEtiquetas(index.get(), n, n.nombre(), primero(n.portadaSubtitulo(), n.lema(), DESCRIPCION),
                imagen(n), baseUrl + "/c/" + actual));
    }

    /** "$14.900 (antes $16.800) · Paquete x 5 · Receta tradicional": el precio solo si el negocio los muestra. */
    private String descripcion(Negocio n, ProductoFila p) {
        String precio = null;
        if (n.mostrarPrecios()) {
            boolean oferta = ReglasProducto.ofertaVigente(p.precioOferta(), p.ofertaDesde(), p.ofertaHasta(), LocalDate.now(clock));
            precio = oferta ? cop(p.precioOferta()) + " (antes " + cop(p.precio()) + ")" : cop(p.precio());
        }
        String texto = Stream.of(precio, p.presentacion(), p.descripcion())
                .filter(v -> v != null && !v.isBlank()).map(String::trim).collect(Collectors.joining(" · "));
        return primero(texto, n.lema(), DESCRIPCION);
    }

    private static String cop(BigDecimal valor) {
        return "$" + NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO")).format(valor);
    }

    private String conEtiquetas(String index, Negocio n, String titulo, String descripcion, String imagen, String url) {
        StringBuilder og = new StringBuilder()
                .append(meta("og:type", "website"))
                .append(meta("og:site_name", "Agorá Catalog"))
                .append(meta("og:locale", "es_CO"))
                .append(meta("og:title", titulo))
                .append(meta("og:description", descripcion))
                .append(meta("og:url", url));
        og.append(meta("og:image", baseUrl + imagen));
        if (imagen.equals(IMAGEN_POR_DEFECTO)) {
            og.append(meta("og:image:width", "1200")).append(meta("og:image:height", "630"));
        }
        og.append("<meta name=\"twitter:card\" content=\"summary_large_image\">\n");
        og.append("<meta name=\"description\" content=\"").append(esc(descripcion)).append("\">\n");
        return index.replace(TITULO, "<title>" + esc(titulo) + "</title>")
                .replace(MARCADOR, og.toString());
    }

    /**
     * La portada en versión liviana (se genera la primera vez); sin portada, el logo, que ya es chico;
     * sin ninguno de los dos, la imagen de Agorá Catalog.
     */
    private String imagen(Negocio n) {
        Path portada = storage.ruta(n.portadaUrl());
        if (portada == null || !Files.isRegularFile(portada)) return logoOPorDefecto(n);
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
            return logoOPorDefecto(n);
        }
    }

    private static String logoOPorDefecto(Negocio n) {
        return n.logoUrl() != null ? n.logoUrl() : IMAGEN_POR_DEFECTO;
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
