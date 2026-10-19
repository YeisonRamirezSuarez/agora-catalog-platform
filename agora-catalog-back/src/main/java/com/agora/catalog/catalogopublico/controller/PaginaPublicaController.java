package com.agora.catalog.catalogopublico.controller;

import com.agora.catalog.catalogopublico.service.PaginaPublicaService;
import com.agora.catalog.catalogopublico.service.PaginaPublicaService.Pagina;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /c/**} del sitio: el proxy (Caddy o serve_app.py) lo reenvía a {@code /api/public/paginas/**}.
 * Abierto por diseño, como todo /public/**.
 */
@RestController
@RequestMapping("/public/paginas")
public class PaginaPublicaController {

    private final PaginaPublicaService service;

    public PaginaPublicaController(PaginaPublicaService service) {
        this.service = service;
    }

    @GetMapping({"/{slugHash}", "/{slugHash}/{subpagina:[a-z-]+}"})
    public ResponseEntity<String> pagina(@PathVariable String slugHash,
                                         @PathVariable(required = false) String subpagina,
                                         HttpServletRequest request) {
        String resto = (subpagina == null ? "" : "/" + subpagina)
                + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
        Pagina p = service.pagina(slugHash, resto);
        if (p.status() == 301) {
            return ResponseEntity.status(301).header(HttpHeaders.LOCATION, p.location()).build();
        }
        // Sin caché: el index.html apunta a los archivos del build actual del front.
        return ResponseEntity.status(p.status())
                .contentType(new MediaType(MediaType.TEXT_HTML, java.nio.charset.StandardCharsets.UTF_8))
                .cacheControl(CacheControl.noCache())
                .body(p.html());
    }
}
