package com.agora.catalog.coleccion.service;

import com.agora.catalog.coleccion.repository.ColeccionRepository;
import com.agora.catalog.coleccion.dto.ColeccionDtos.ColeccionRequest;
import com.agora.catalog.coleccion.dto.ColeccionDtos.ColeccionResponse;
import com.agora.catalog.common.exception.ResourceNotFoundException;
import com.agora.catalog.storage.ImagenService;
import com.agora.catalog.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

/** Colecciones («¿Cuál es el plan?»): hasta 6 por empresa, solo con productos propios. */
@Service
public class ColeccionService {

    static final int MAXIMO = 6;

    private final ColeccionRepository repository;
    private final ImagenService imagenes;
    private final StorageService storage;

    public ColeccionService(ColeccionRepository repository, ImagenService imagenes, StorageService storage) {
        this.repository = repository;
        this.imagenes = imagenes;
        this.storage = storage;
    }

    public List<ColeccionResponse> list(long empresaId) {
        return repository.findAll(empresaId);
    }

    public ColeccionResponse get(long empresaId, long id) {
        return repository.findById(empresaId, id).orElseThrow(() -> new ResourceNotFoundException("La colección no existe"));
    }

    @Transactional
    public ColeccionResponse crear(long empresaId, ColeccionRequest r) {
        if (repository.count(empresaId) >= MAXIMO) {
            throw new IllegalArgumentException("Puedes tener hasta " + MAXIMO + " colecciones. Elimina una para crear otra.");
        }
        List<Long> productos = productos(empresaId, r.productos());
        long id = repository.insert(empresaId, r.nombre().trim(), blank(r.descripcion()), r.visible());
        repository.setProductos(id, productos);
        return get(empresaId, id);
    }

    @Transactional
    public ColeccionResponse actualizar(long empresaId, long id, ColeccionRequest r) {
        get(empresaId, id);
        List<Long> productos = productos(empresaId, r.productos());
        repository.update(empresaId, id, r.nombre().trim(), blank(r.descripcion()), r.visible());
        repository.setProductos(id, productos);
        return get(empresaId, id);
    }

    @Transactional
    public void eliminar(long empresaId, long id) {
        ColeccionResponse c = get(empresaId, id);
        repository.delete(empresaId, id);
        storage.borrar(c.imagenUrl());
    }

    @Transactional
    public List<ColeccionResponse> ordenar(long empresaId, List<Long> ids) {
        var propias = new HashSet<>(repository.findAll(empresaId).stream().map(ColeccionResponse::id).toList());
        if (ids.size() != propias.size() || !propias.equals(new HashSet<>(ids))) {
            throw new IllegalArgumentException("El orden debe incluir todas tus colecciones, una sola vez");
        }
        for (int i = 0; i < ids.size(); i++) repository.setOrden(empresaId, ids.get(i), i + 1);
        return repository.findAll(empresaId);
    }

    /** Foto de la tarjeta del plan: JPG de hasta 1200 px. */
    @Transactional
    public ColeccionResponse subirImagen(long empresaId, long id, byte[] archivo) {
        ColeccionResponse c = get(empresaId, id);
        String url = storage.guardar(empresaId, "colecciones", imagenes.producto(archivo).grande());
        repository.setImagen(empresaId, id, url);
        storage.borrar(c.imagenUrl());
        return get(empresaId, id);
    }

    /** Sin repetidos y todos de la empresa: un id ajeno se trata como inexistente. */
    private List<Long> productos(long empresaId, List<Long> ids) {
        List<Long> unicos = List.copyOf(new LinkedHashSet<>(ids));
        if (repository.productosPropios(empresaId, unicos) != unicos.size()) {
            throw new ResourceNotFoundException("Uno de los productos no existe");
        }
        return unicos;
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
