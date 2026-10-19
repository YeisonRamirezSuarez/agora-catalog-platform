package com.agora.catalog.importacion.dto;

import java.util.List;

/** Respuestas de /admin/importacion. */
public final class ImportacionDtos {

    private ImportacionDtos() {
    }

    /** {@code fila} es el número de fila del Excel, como lo ve el dueño (la 1 es la de los títulos). */
    public record ErrorFila(int fila, String mensaje) {
    }

    /**
     * Resultado de revisar (o aplicar) un Excel. Con errores no se aplica nada: el dueño corrige el
     * archivo y lo vuelve a subir.
     */
    public record ReporteImportacion(
            int filas,
            int nuevos,
            int actualizados,
            List<String> categoriasNuevas,
            List<ErrorFila> errores,
            boolean aplicado
    ) {
    }

    public record ErrorArchivo(String archivo, String mensaje) {
    }

    /** Resultado de subir un ZIP de fotos: cada foto va al producto cuyo código es el nombre del archivo. */
    public record ReporteImagenes(
            List<String> asignadas,
            List<String> sinProducto,
            List<ErrorArchivo> conError
    ) {
    }
}
