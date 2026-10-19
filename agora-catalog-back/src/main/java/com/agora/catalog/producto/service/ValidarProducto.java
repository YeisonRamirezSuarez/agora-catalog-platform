package com.agora.catalog.producto.service;

import com.agora.catalog.producto.dto.ProductoDtos.PresentacionDto;
import com.agora.catalog.producto.dto.ProductoDtos.ProductoRequest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        if (r.presentaciones() != null && !r.presentaciones().isEmpty()) {
            if (r.presentacion() == null || r.presentacion().isBlank()) {
                errores.add("Escribe la presentación principal (ej. Libra) para agregar otras");
            }
            Set<String> nombres = new HashSet<>();
            if (r.presentacion() != null) nombres.add(r.presentacion().trim().toLowerCase());
            for (PresentacionDto v : r.presentaciones()) {
                if (v.nombre() != null && !nombres.add(v.nombre().trim().toLowerCase())) {
                    errores.add("La presentación «" + v.nombre().trim() + "» está repetida");
                }
                if (v.precioOferta() != null && v.precio() != null && v.precioOferta() >= v.precio()) {
                    errores.add("La oferta de «" + v.nombre() + "» debe ser menor que su precio");
                }
            }
        }
        return errores;
    }
}
