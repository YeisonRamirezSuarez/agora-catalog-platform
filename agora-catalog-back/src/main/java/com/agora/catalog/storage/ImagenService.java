package com.agora.catalog.storage;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/**
 * Lee y prepara las fotos que suben los negocios. Entrada JPG, PNG o WebP; salida siempre JPG.
 *
 * <p><b>Por qué se miden antes de decodificar.</b> Un PNG de pocos MB puede declarar 20 000 × 20 000
 * píxeles y ocupar gigas de RAM al abrirse: una sola foto así tumbaría la API de todas las empresas.
 * El ancho y el alto se leen de la cabecera ({@link ImageReader#getWidth}) y se rechaza antes de leer
 * un solo píxel.
 */
@Service
public class ImagenService {

    public static final long MAX_BYTES = 5L * 1024 * 1024;
    public static final int MAX_LADO = 8000;
    private static final Set<String> FORMATOS = Set.of("jpeg", "png", "webp");
    private static final float CALIDAD = 0.85f;

    /** Rectángulo de recorte en píxeles de la imagen original. */
    public record Recorte(int x, int y, int ancho, int alto) {
    }

    /** Foto de producto: grande (hasta 1200 px) y miniatura (400 px), en JPG. */
    public record FotoProducto(byte[] grande, byte[] miniatura) {
    }

    /** Valida tamaño, formato y dimensiones, y recién entonces decodifica. */
    public BufferedImage leer(byte[] archivo) {
        if (archivo == null || archivo.length == 0) {
            throw new IllegalArgumentException("Elige una foto");
        }
        if (archivo.length > MAX_BYTES) {
            throw new IllegalArgumentException("La foto pesa más de 5 MB. Elige una más liviana.");
        }
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(archivo))) {
            Iterator<ImageReader> readers = in == null ? null : ImageIO.getImageReaders(in);
            if (readers == null || !readers.hasNext()) {
                throw new IllegalArgumentException("El archivo no es una foto JPG, PNG o WebP");
            }
            ImageReader reader = readers.next();
            try {
                String formato = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!FORMATOS.contains(formato)) {
                    throw new IllegalArgumentException("El archivo no es una foto JPG, PNG o WebP");
                }
                reader.setInput(in, true, true);
                int ancho = reader.getWidth(0), alto = reader.getHeight(0);
                if (ancho > MAX_LADO || alto > MAX_LADO) {
                    throw new IllegalArgumentException("La foto mide " + ancho + " × " + alto
                            + " px; el máximo es " + MAX_LADO + " px por lado.");
                }
                return aRgb(reader.read(0));
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("No pudimos leer la foto. Prueba con otra.", e);
        }
    }

    public FotoProducto producto(byte[] archivo) {
        BufferedImage img = leer(archivo);
        return new FotoProducto(ajustar(img, 1200), ajustar(img, 400));
    }

    /**
     * Recorta y lleva al tamaño exacto. Con {@code puedeSalirse} (el logo alejado para que entre
     * completo) lo que queda fuera de la imagen se rellena de blanco; si no, el recorte debe quedar dentro.
     */
    public byte[] recortar(BufferedImage img, Recorte r, int anchoFinal, int altoFinal, boolean puedeSalirse) {
        if (r.ancho() <= 0 || r.alto() <= 0) {
            throw new IllegalArgumentException("El recorte no es válido");
        }
        boolean dentro = r.x() >= 0 && r.y() >= 0 && r.x() + r.ancho() <= img.getWidth() && r.y() + r.alto() <= img.getHeight();
        if (!dentro && !puedeSalirse) {
            throw new IllegalArgumentException("El recorte se sale de la foto");
        }
        if (r.ancho() > MAX_LADO * 2 || r.alto() > MAX_LADO * 2) {
            throw new IllegalArgumentException("El recorte es demasiado grande");
        }
        BufferedImage salida = new BufferedImage(anchoFinal, altoFinal, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = salida.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, anchoFinal, altoFinal);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            double escalaX = (double) anchoFinal / r.ancho(), escalaY = (double) altoFinal / r.alto();
            g.drawImage(img, (int) Math.round(-r.x() * escalaX), (int) Math.round(-r.y() * escalaY),
                    (int) Math.round(img.getWidth() * escalaX), (int) Math.round(img.getHeight() * escalaY), null);
        } finally {
            g.dispose();
        }
        return jpg(salida);
    }

    /** Lleva el lado mayor a {@code max} sin agrandar; siempre JPG. */
    private byte[] ajustar(BufferedImage img, int max) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            var b = Thumbnails.of(img);
            if (img.getWidth() > max || img.getHeight() > max) b.size(max, max); else b.scale(1);
            b.outputFormat("jpg").outputQuality(CALIDAD).toOutputStream(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private byte[] jpg(BufferedImage img) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(img).scale(1).outputFormat("jpg").outputQuality(CALIDAD).toOutputStream(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** JPG no tiene transparencia: un PNG con fondo transparente queda sobre blanco, no sobre negro. */
    private static BufferedImage aRgb(BufferedImage img) {
        if (img.getType() == BufferedImage.TYPE_INT_RGB) return img;
        BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, img.getWidth(), img.getHeight());
            g.drawImage(img, 0, 0, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }
}
