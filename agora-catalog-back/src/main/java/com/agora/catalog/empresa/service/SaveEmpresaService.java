package com.agora.catalog.empresa.service;

import com.agora.catalog.common.exception.DuplicateResourceException;
import com.agora.catalog.common.exception.ResourceNotFoundException;
import com.agora.catalog.common.random.RandomCodes;
import com.agora.catalog.empresa.model.Empresa;
import com.agora.catalog.empresa.repository.EmpresaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;

/** Crear y editar empresas (nombre y slug). El hash público se genera una vez y nunca cambia. */
@Service
public class SaveEmpresaService {

    private final EmpresaRepository repository;

    public SaveEmpresaService(EmpresaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public long create(String nombre, String slug) {
        String finalSlug = resolveSlug(nombre, slug, null);
        String hash = RandomCodes.publicHash();
        // 36^8 combinaciones: un choque es casi imposible, pero no cuesta reintentar.
        for (int intento = 0; repository.existsByPublicHash(hash); intento++) {
            if (intento == 5) throw new IllegalStateException("No se pudo generar un link único; intenta de nuevo");
            hash = RandomCodes.publicHash();
        }
        return repository.insert(nombre.trim(), finalSlug, hash);
    }

    @Transactional
    public void update(long id, String nombre, String slug) {
        require(repository, id);
        repository.update(id, nombre.trim(), resolveSlug(nombre, slug, id));
    }

    private String resolveSlug(String nombre, String slug, Long exceptId) {
        String value = slug == null || slug.isBlank() ? slugify(nombre) : slug;
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Escribe un slug: el nombre no tiene letras ni números");
        }
        if (repository.existsBySlug(value, exceptId)) {
            throw new DuplicateResourceException("Ya existe una empresa con el slug " + value);
        }
        return value;
    }

    /** "Salsamentaria Javier & Cía." -> "salsamentaria-javier-cia" */
    static String slugify(String text) {
        String plain = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = plain.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return slug.length() > 80 ? slug.substring(0, 80).replaceAll("-+$", "") : slug;
    }

    static Empresa require(EmpresaRepository repository, long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("La empresa no existe"));
    }
}
