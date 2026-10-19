package com.agora.catalog.empresa.service;

import com.agora.catalog.common.exception.DuplicateResourceException;
import com.agora.catalog.common.exception.ResourceNotFoundException;
import com.agora.catalog.empresa.dto.ConfiguracionDtos.ConfiguracionRequest;
import com.agora.catalog.empresa.dto.ConfiguracionDtos.ConfiguracionResponse;
import com.agora.catalog.empresa.dto.ConfiguracionDtos.Recorte;
import com.agora.catalog.empresa.dto.ConfiguracionDtos.WhatsappDto;
import com.agora.catalog.empresa.repository.ConfiguracionRepository;
import com.agora.catalog.empresa.repository.EmpresaRepository;
import com.agora.catalog.storage.ImagenService;
import com.agora.catalog.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;

/** Configuración del catálogo que edita el administrador del negocio: textos, colores, WhatsApp, sellos y fotos. */
@Service
public class ConfiguracionService {

    /** Tamaño final del logo (cuadrado) y de la portada (nítida en pantallas retina). */
    static final int LOGO = 512, PORTADA_ANCHO = 3200, PORTADA_ALTO = 1508;

    private final ConfiguracionRepository repository;
    private final EmpresaRepository empresas;
    private final ImagenService imagenes;
    private final StorageService storage;

    public ConfiguracionService(ConfiguracionRepository repository, EmpresaRepository empresas, ImagenService imagenes,
                                StorageService storage) {
        this.repository = repository;
        this.empresas = empresas;
        this.imagenes = imagenes;
        this.storage = storage;
    }

    public ConfiguracionResponse get(long empresaId) {
        return repository.find(empresaId).orElseThrow(() -> new ResourceNotFoundException("La empresa no existe"));
    }

    @Transactional
    public ConfiguracionResponse guardar(long empresaId, ConfiguracionRequest r) {
        get(empresaId);
        // Botones con texto blanco y acento sobre la portada noche: el panel ya avisa; acá no se guarda sin contraste.
        if (Contraste.ratio(r.colorPrimario(), "#FFFFFF") < Contraste.MINIMO) {
            throw new IllegalArgumentException("El texto blanco no se lee sobre el color de botones. Elige un tono más oscuro.");
        }
        if (Contraste.ratio(r.colorSecundario(), Contraste.NOCHE) < Contraste.MINIMO) {
            throw new IllegalArgumentException("El color de acento no se lee sobre la portada oscura. Elige un tono más claro.");
        }
        var numeros = new HashSet<String>();
        for (WhatsappDto w : r.whatsapps()) {
            if (!numeros.add(w.numero())) throw new IllegalArgumentException("El WhatsApp " + w.numero() + " está repetido");
        }
        if (empresas.existsBySlug(r.slug(), empresaId)) {
            throw new DuplicateResourceException("El slug " + r.slug() + " ya lo usa otro negocio");
        }
        repository.update(empresaId, r);
        return get(empresaId);
    }

    @Transactional
    public ConfiguracionResponse guardarColecciones(long empresaId, String titulo, String texto, String nota) {
        get(empresaId);
        repository.updateColecciones(empresaId, titulo, texto, nota);
        return get(empresaId);
    }

    /**
     * Logo o portada con su recorte. Con archivo nuevo se guarda también el original (para reajustar el
     * encuadre después); sin archivo, se recorta de nuevo el original guardado. El logo puede alejarse
     * para que entre completo (lo que sobra queda en blanco); la portada debe quedar dentro de la foto.
     */
    @Transactional
    public ConfiguracionResponse guardarFoto(long empresaId, String tipo, byte[] archivo, Recorte recorte) {
        ConfiguracionResponse actual = get(empresaId);
        boolean logo = tipo.equals("logo");
        String originalActual = logo ? actual.logoOriginalUrl() : actual.portadaOriginalUrl();
        String finalActual = logo ? actual.logoUrl() : actual.portadaUrl();

        BufferedImage img;
        String original;
        if (archivo != null && archivo.length > 0) {
            img = imagenes.leer(archivo);
            original = storage.guardar(empresaId, "originales", jpg(img));
        } else if (originalActual != null) {
            img = leerGuardada(originalActual);
            original = originalActual;
        } else {
            throw new IllegalArgumentException("Elige una foto");
        }
        if (recorte == null) {
            var c = logo ? centrado(img, 1, 1) : centrado(img, PORTADA_ANCHO, PORTADA_ALTO);
            recorte = new Recorte(c.x(), c.y(), c.ancho(), c.alto());
        }
        byte[] salida = logo
                ? imagenes.recortar(img, rect(recorte), LOGO, LOGO, true)
                : imagenes.recortar(img, rect(recorte), PORTADA_ANCHO, PORTADA_ALTO, false);
        String url = storage.guardar(empresaId, logo ? "logo" : "portada", salida);
        repository.setFoto(empresaId, tipo, url, original, recorte);
        storage.borrar(finalActual);
        if (originalActual != null && !originalActual.equals(original)) storage.borrar(originalActual);
        return get(empresaId);
    }

    @Transactional
    public ConfiguracionResponse quitarFoto(long empresaId, String tipo) {
        ConfiguracionResponse actual = get(empresaId);
        boolean logo = tipo.equals("logo");
        repository.setFoto(empresaId, tipo, null, null, null);
        storage.borrar(logo ? actual.logoUrl() : actual.portadaUrl());
        storage.borrar(logo ? actual.logoOriginalUrl() : actual.portadaOriginalUrl());
        return get(empresaId);
    }

    private BufferedImage leerGuardada(String url) {
        try {
            return ImageIO.read(storage.root().resolve(url.substring(StorageService.URL_PREFIX.length())).toFile());
        } catch (IOException e) {
            throw new IllegalArgumentException("No encontramos la foto guardada: súbela de nuevo", e);
        }
    }

    private static ImagenService.Recorte rect(Recorte r) {
        return new ImagenService.Recorte(r.x(), r.y(), r.ancho(), r.alto());
    }

    private static ImagenService.Recorte centrado(BufferedImage img, int propAncho, int propAlto) {
        double prop = (double) propAncho / propAlto;
        int w = img.getWidth(), h = (int) Math.round(w / prop);
        if (h > img.getHeight()) {
            h = img.getHeight();
            w = (int) Math.round(h * prop);
        }
        return new ImagenService.Recorte((img.getWidth() - w) / 2, (img.getHeight() - h) / 2, w, h);
    }

    private static byte[] jpg(BufferedImage img) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
