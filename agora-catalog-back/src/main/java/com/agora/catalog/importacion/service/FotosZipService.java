package com.agora.catalog.importacion.service;

import com.agora.catalog.importacion.dto.ImportacionDtos.ErrorArchivo;
import com.agora.catalog.importacion.dto.ImportacionDtos.ReporteImagenes;
import com.agora.catalog.producto.repository.ProductoRepository;
import com.agora.catalog.producto.service.ProductoService;
import com.agora.catalog.storage.ImagenService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Fotos en lote: cada archivo del ZIP va al producto cuyo código es su nombre (QUE-001.jpg → QUE-001).
 * Cada foto se guarda por su cuenta: una mala no tumba las demás. El ZIP no se descomprime a disco;
 * se lee entrada por entrada con tope de tamaño, para que un ZIP bomba no llene la memoria.
 */
@Service
public class FotosZipService {

    public static final long MAX_BYTES = 50L * 1024 * 1024;
    static final int MAX_ENTRADAS = 500;
    private static final Set<String> EXTENSIONES = Set.of("jpg", "jpeg", "png", "webp");

    private final ProductoRepository productos;
    private final ProductoService productoService;

    public FotosZipService(ProductoRepository productos, ProductoService productoService) {
        this.productos = productos;
        this.productoService = productoService;
    }

    public ReporteImagenes asignar(long empresaId, byte[] zip) {
        if (zip == null || zip.length == 0) throw new IllegalArgumentException("Elige el archivo ZIP");
        if (zip.length > MAX_BYTES) throw new IllegalArgumentException("El ZIP pesa más de 50 MB. Súbelo en partes.");

        Map<String, Long> ids = new HashMap<>();
        for (ProductoRepository.Fila p : productos.findAll(empresaId)) ids.put(p.codigo().toLowerCase(Locale.ROOT), p.id());

        List<String> asignadas = new ArrayList<>();
        List<String> sinProducto = new ArrayList<>();
        List<ErrorArchivo> conError = new ArrayList<>();
        Set<Long> yaTienen = new HashSet<>();
        int entradas = 0;
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            // shortcut: saltar una entrada la descomprime (sin guardarla); el tope de 50 MB del ZIP acota
            // la CPU. Si se abre a más usuarios, cortar también por bytes descomprimidos totales.
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                if (++entradas > MAX_ENTRADAS) {
                    throw new IllegalArgumentException("El ZIP tiene más de " + MAX_ENTRADAS + " archivos. Súbelo en partes.");
                }
                String ruta = e.getName().replace('\\', '/');
                String nombre = ruta.substring(ruta.lastIndexOf('/') + 1);
                // Carpetas, basura de macOS y archivos ocultos no son fotos.
                if (e.isDirectory() || ruta.startsWith("__MACOSX/") || nombre.isEmpty() || nombre.startsWith(".")) continue;

                int punto = nombre.lastIndexOf('.');
                String extension = punto < 0 ? "" : nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
                if (!EXTENSIONES.contains(extension)) {
                    conError.add(new ErrorArchivo(nombre, "No es una foto JPG, PNG o WebP"));
                    continue;
                }
                Long id = ids.get(nombre.substring(0, punto).trim().toLowerCase(Locale.ROOT));
                if (id == null) {
                    sinProducto.add(nombre);
                    continue;
                }
                if (!yaTienen.add(id)) {
                    conError.add(new ErrorArchivo(nombre, "Ya hay otra foto para este código en el ZIP"));
                    continue;
                }
                // Se lee un byte de más para saber si la entrada pasa el tope sin descomprimirla entera.
                byte[] foto = in.readNBytes((int) ImagenService.MAX_BYTES + 1);
                if (foto.length > ImagenService.MAX_BYTES) {
                    conError.add(new ErrorArchivo(nombre, "La foto pesa más de 5 MB"));
                    continue;
                }
                try {
                    productoService.subirImagen(empresaId, id, foto);
                    asignadas.add(nombre);
                } catch (IllegalArgumentException ex) {
                    conError.add(new ErrorArchivo(nombre, ex.getMessage()));
                }
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("No pudimos abrir el ZIP. Revisa que no esté dañado.", ex);
        }
        if (entradas == 0) throw new IllegalArgumentException("El archivo no es un ZIP o está vacío");
        return new ReporteImagenes(asignadas, sinProducto, conError);
    }
}
