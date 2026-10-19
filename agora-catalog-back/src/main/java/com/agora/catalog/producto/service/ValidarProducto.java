package com.agora.catalog.producto.service;

import com.agora.catalog.producto.dto.ProductoDtos.ProductoRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Reglas de negocio de un producto que no caben en las anotaciones (ESPECIFICACION.md §4). Un solo
 * lugar para el formulario del panel y para cada fila del Excel: los dos dicen lo mismo.
 */
public final class ValidarProducto {

    private ValidarProducto() {
    }

    /** Errores en español, listos para mostrar; vacía si el producto es válido. */
    public static List<String> errores(ProductoRequest r) {
        List<String> errores = new ArrayList<>();
        if (r.precioOferta() != null && r.precio() != null && r.precioOferta() >= r.precio()) {
            errores.add("El precio de oferta debe ser menor que el precio");
        }
        if (r.precioOferta() == null && (r.ofertaDesde() != null || r.ofertaHasta() != null)) {
            errores.add("Escribe el precio de oferta o quita las fechas de la oferta");
        }
        if (r.ofertaDesde() != null && r.ofertaHasta() != null && r.ofertaDesde().isAfter(r.ofertaHasta())) {
            errores.add("La oferta termina antes de empezar: revisa las fechas");
        }
        return errores;
    }
}
