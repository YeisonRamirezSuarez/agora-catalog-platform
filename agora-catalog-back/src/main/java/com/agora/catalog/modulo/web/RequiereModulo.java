package com.agora.catalog.modulo.web;

import com.agora.catalog.modulo.model.Modulo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Las rutas de este controlador (o de este método) solo responden si la empresa del token tiene el
 * módulo encendido; si no, 403 con un mensaje para el usuario. Lo revisa {@link ModuloInterceptor}.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiereModulo {
    Modulo value();
}
