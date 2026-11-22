package com.agora.catalog.modulo.dto;

/** Un módulo con su nombre para mostrar y si la empresa lo tiene encendido. */
public record ModuloResponse(String modulo, String nombre, String descripcion, boolean activo) {
}
