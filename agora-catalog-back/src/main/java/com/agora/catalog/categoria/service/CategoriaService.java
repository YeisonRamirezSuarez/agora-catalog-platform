package com.agora.catalog.categoria.service;

import com.agora.catalog.categoria.dto.CategoriaDtos.CategoriaResponse;
import com.agora.catalog.categoria.dto.CategoriaDtos.FranjaRequest;
import com.agora.catalog.categoria.repository.CategoriaRepository;
import com.agora.catalog.common.exception.DuplicateResourceException;
import com.agora.catalog.common.exception.ResourceInUseException;
import com.agora.catalog.common.exception.ResourceNotFoundException;
import com.agora.catalog.storage.ImagenService;
import com.agora.catalog.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

/** Categorías de la empresa del token, con su orden y la franja grande del inicio. */
@Service
public class CategoriaService {

    private final CategoriaRepository repository;
    private final ImagenService imagenes;
    private final StorageService storage;

    public CategoriaService(CategoriaRepository repository, ImagenService imagenes, StorageService storage) {
        this.repository = repository;
        this.imagenes = imagenes;
        this.storage = storage;
    }

    public List<CategoriaResponse> list(long empresaId) {
        return repository.findAll(empresaId);
    }

    public CategoriaResponse get(long empresaId, long id) {
        return repository.findById(empresaId, id).orElseThrow(() -> new ResourceNotFoundException("La categoría no existe"));
    }

    @Transactional
    public CategoriaResponse crear(long empresaId, String nombre, boolean activa) {
        String limpio = nombre.trim();
        unico(empresaId, limpio, null);
        return get(empresaId, repository.insert(empresaId, limpio, activa));
    }

    @Transactional
    public CategoriaResponse actualizar(long empresaId, long id, String nombre, boolean activa) {
        get(empresaId, id);
        String limpio = nombre.trim();
        unico(empresaId, limpio, id);
        repository.update(empresaId, id, limpio, activa);
        return get(empresaId, id);
    }

    /** Solo vacía: los productos no se borran por arrastre sin que el dueño lo sepa. */
    @Transactional
    public void eliminar(long empresaId, long id) {
        CategoriaResponse c = get(empresaId, id);
        if (c.productos() > 0) {
            throw new ResourceInUseException("«" + c.nombre() + "» tiene " + c.productos()
                    + (c.productos() == 1 ? " producto" : " productos") + ". Muévelos a otra categoría o elimínalos primero.");
        }
        repository.delete(empresaId, id);
        storage.borrar(c.bannerUrl());
    }

    /** Recibe todas las categorías en el orden nuevo (lo que deja el arrastrar y soltar del panel). */
    @Transactional
    public List<CategoriaResponse> ordenar(long empresaId, List<Long> ids) {
        List<CategoriaResponse> actuales = repository.findAll(empresaId);
        var propias = new HashSet<>(actuales.stream().map(CategoriaResponse::id).toList());
        if (ids.size() != propias.size() || !propias.equals(new HashSet<>(ids))) {
            throw new IllegalArgumentException("El orden debe incluir todas tus categorías, una sola vez");
        }
        for (int i = 0; i < ids.size(); i++) {
            repository.setOrden(empresaId, ids.get(i), i + 1);
        }
        return repository.findAll(empresaId);
    }

    /**
     * Enciende la franja en esta categoría (y la apaga en la que la tuviera). Sin foto nueva se
     * conserva la que ya tenía; la primera vez la foto es obligatoria. Salida JPG 3200 × 1200.
     */
    @Transactional
    public CategoriaResponse guardarFranja(long empresaId, long id, FranjaRequest r, byte[] foto, ImagenService.Recorte recorte) {
        CategoriaResponse c = get(empresaId, id);
        String url = c.bannerUrl();
        if (foto != null && foto.length > 0) {
            var img = imagenes.leer(foto);
            var rect = recorte != null ? recorte : centrado(img.getWidth(), img.getHeight(), 3200, 1200);
            url = storage.guardar(empresaId, "franjas", imagenes.recortar(img, rect, 3200, 1200, false));
        } else if (url == null) {
            throw new IllegalArgumentException("Elige una foto para la franja");
        }
        repository.setFranja(empresaId, id, url, vacioANull(r.titulo()), vacioANull(r.frase()), vacioANull(r.nota()));
        if (c.bannerUrl() != null && !c.bannerUrl().equals(url)) storage.borrar(c.bannerUrl());
        return get(empresaId, id);
    }

    @Transactional
    public CategoriaResponse quitarFranja(long empresaId, long id) {
        get(empresaId, id);
        repository.quitarFranja(empresaId, id);
        return get(empresaId, id);
    }

    private void unico(long empresaId, String nombre, Long exceptId) {
        if (repository.existsByNombre(empresaId, nombre, exceptId)) {
            throw new DuplicateResourceException("Ya tienes una categoría llamada «" + nombre + "»");
        }
    }

    /** El recorte más grande con la proporción pedida, centrado. */
    public static ImagenService.Recorte centrado(int ancho, int alto, int propAncho, int propAlto) {
        double prop = (double) propAncho / propAlto;
        int w = ancho, h = (int) Math.round(ancho / prop);
        if (h > alto) {
            h = alto;
            w = (int) Math.round(alto * prop);
        }
        return new ImagenService.Recorte((ancho - w) / 2, (alto - h) / 2, w, h);
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
