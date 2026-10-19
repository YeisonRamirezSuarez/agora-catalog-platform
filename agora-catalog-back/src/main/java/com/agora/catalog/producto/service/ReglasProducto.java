package com.agora.catalog.producto.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Reglas de precio y etiquetas del producto (ESPECIFICACION.md §4), en un solo lugar para el
 * catálogo público, el panel y la importación. Reciben la fecha de afuera (un {@code Clock}) para
 * poder probarlas.
 */
public final class ReglasProducto {

    /** Un producto es "nuevo" durante estos días desde que se creó. */
    public static final int DIAS_NUEVO = 30;

    private ReglasProducto() {
    }

    /** Vigente si hay precio de oferta y hoy está entre las fechas; una fecha que falta deja ese extremo abierto. */
    public static boolean ofertaVigente(BigDecimal precioOferta, LocalDate desde, LocalDate hasta, LocalDate hoy) {
        return precioOferta != null
                && (desde == null || !hoy.isBefore(desde))
                && (hasta == null || !hoy.isAfter(hasta));
    }

    public static boolean esNuevo(Instant creadoEn, Instant ahora) {
        return creadoEn != null && Duration.between(creadoEn, ahora).toDays() < DIAS_NUEVO;
    }

    /** Porcentaje de descuento redondeado al entero más cercano, para la etiqueta "Promo -X%". */
    public static int descuento(BigDecimal precio, BigDecimal precioOferta) {
        if (precio == null || precioOferta == null || precio.signum() <= 0) return 0;
        return BigDecimal.ONE.subtract(precioOferta.divide(precio, 4, RoundingMode.HALF_UP))
                .multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
    }
}
