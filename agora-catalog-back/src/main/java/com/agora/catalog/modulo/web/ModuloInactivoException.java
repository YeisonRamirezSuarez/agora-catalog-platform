package com.agora.catalog.modulo.web;

import com.agora.catalog.modulo.model.Modulo;

/** La empresa no tiene el módulo que pide la ruta (403). */
public class ModuloInactivoException extends RuntimeException {

    public ModuloInactivoException(Modulo modulo) {
        super("Tu plan no incluye «" + modulo.nombre() + "». Escríbenos para activarlo.");
    }
}
