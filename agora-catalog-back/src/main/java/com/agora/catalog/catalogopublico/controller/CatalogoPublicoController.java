package com.agora.catalog.catalogopublico.controller;

import com.agora.catalog.catalogopublico.dto.response.CatalogoPublicoResponse;
import com.agora.catalog.catalogopublico.service.GetCatalogoPublicoService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/** Catálogo público por link: sin login, por diseño (/public/** está abierto en SecurityConfig). */
@RestController
@RequestMapping("/public/catalogos")
public class CatalogoPublicoController {

    private final GetCatalogoPublicoService service;

    public CatalogoPublicoController(GetCatalogoPublicoService service) {
        this.service = service;
    }

    // Caché corta: un cambio de precio se ve en un minuto y los picos de visitas no pegan todos en la base.
    @GetMapping("/{publicHash}")
    public ResponseEntity<CatalogoPublicoResponse> get(@PathVariable String publicHash) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
                .body(service.get(publicHash));
    }
}
