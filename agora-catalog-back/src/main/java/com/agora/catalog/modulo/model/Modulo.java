package com.agora.catalog.modulo.model;

/**
 * Lo que se vende por separado. El catálogo (productos, categorías, configuración y link público)
 * no está acá: lo tiene toda empresa. Sin fila en {@code empresa_modulo}, el módulo está apagado.
 */
public enum Modulo {

    PEDIDO_WHATSAPP("Pedido por WhatsApp", "El visitante arma un pedido en el catálogo y lo envía por WhatsApp."),
    IMPORTAR_EXCEL("Importar desde Excel", "Plantilla, importar y exportar productos, y fotos en un ZIP."),
    COLECCIONES("Colecciones", "Secciones de productos destacados en el catálogo."),
    ESTADISTICAS("Estadísticas", "Visitas, pedidos y productos más vistos en el inicio del panel."),
    VENTA_RUTA("Venta en ruta", "Carros con stock, tiendas y pedidos a tiendas con comprobante.");

    private final String nombre;
    private final String descripcion;

    Modulo(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public String nombre() {
        return nombre;
    }

    public String descripcion() {
        return descripcion;
    }
}
