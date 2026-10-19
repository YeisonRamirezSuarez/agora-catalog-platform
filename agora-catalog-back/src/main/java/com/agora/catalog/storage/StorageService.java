package com.agora.catalog.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Fotos en el disco del servidor ({@code STORAGE_PATH}), servidas en {@code /api/uploads/**}.
 *
 * <p>Cada archivo lleva un nombre aleatorio y nunca se sobrescribe: cambiar una foto crea un archivo
 * nuevo y borra el anterior, así el navegador puede guardarlas en caché para siempre.
 */
// shortcut: disco local de un solo servidor; con dos servidores o mucho tráfico, pasar a S3/R2 (ESPECIFICACION §12).
@Slf4j
@Service
public class StorageService {

    /** Prefijo público de las URL que se guardan en la base. */
    public static final String URL_PREFIX = "/api/uploads/";

    private final Path root;

    public StorageService(@Value("${settings.storage-path:./uploads}") String storagePath) {
        this.root = Path.of(storagePath).toAbsolutePath().normalize();
    }

    public Path root() {
        return root;
    }

    /** Guarda un JPG de la empresa y devuelve su URL pública. */
    public String guardar(long empresaId, String carpeta, byte[] jpg) {
        String relativo = empresaId + "/" + carpeta + "/" + UUID.randomUUID() + ".jpg";
        Path destino = root.resolve(relativo);
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, jpg);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la foto", e);
        }
        return URL_PREFIX + relativo;
    }

    /** Borra el archivo de una URL propia. Nunca falla: una foto huérfana no debe deshacer un cambio. */
    public void borrar(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) return;
        Path archivo = root.resolve(url.substring(URL_PREFIX.length())).normalize();
        if (!archivo.startsWith(root)) return; // nada fuera de la carpeta
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            log.warn("[STORAGE] No se pudo borrar {}: {}", archivo, e.getMessage());
        }
    }
}
