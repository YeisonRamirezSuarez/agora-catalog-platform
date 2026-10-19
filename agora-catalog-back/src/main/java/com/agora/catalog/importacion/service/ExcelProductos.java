package com.agora.catalog.importacion.service;

import com.agora.catalog.producto.repository.ProductoRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** La plantilla de productos y la exportación, en el mismo formato: se exporta, se edita y se vuelve a subir. */
public final class ExcelProductos {

    public static final String HOJA = "Productos";
    /** Columnas en orden; las obligatorias llevan asterisco en el título. */
    public static final List<String> COLUMNAS = List.of("codigo", "categoria", "nombre", "descripcion", "presentacion",
            "unidad_venta", "precio", "precio_oferta", "oferta_desde", "oferta_hasta", "disponible", "destacado");
    static final List<String> OBLIGATORIAS = List.of("codigo", "categoria", "nombre", "unidad_venta", "precio");
    public static final List<String> UNIDADES = List.of("UNIDAD", "LIBRA", "KILO", "GRAMOS", "PAQUETE", "CAJA");
    static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final int MAX_FILAS = 2000;
    private static final int[] ANCHOS = {14, 18, 34, 40, 20, 14, 12, 14, 14, 14, 12, 12};

    private ExcelProductos() {
    }

    public static byte[] plantilla() {
        return libro(List.of());
    }

    public static byte[] exportar(List<ProductoRepository.Fila> productos) {
        return libro(productos);
    }

    private static byte[] libro(List<ProductoRepository.Fila> productos) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = wb.createSheet(HOJA);
            CellStyle titulo = wb.createCellStyle();
            Font negrita = wb.createFont();
            negrita.setBold(true);
            titulo.setFont(negrita);
            Row cabecera = hoja.createRow(0);
            for (int i = 0; i < COLUMNAS.size(); i++) {
                String c = COLUMNAS.get(i);
                Cell cell = cabecera.createCell(i);
                cell.setCellValue(OBLIGATORIAS.contains(c) ? c + "*" : c);
                cell.setCellStyle(titulo);
                hoja.setColumnWidth(i, ANCHOS[i] * 256);
            }
            hoja.createFreezePane(0, 1);

            int r = 1;
            for (ProductoRepository.Fila p : productos) {
                Row row = hoja.createRow(r++);
                row.createCell(0).setCellValue(p.codigo());
                row.createCell(1).setCellValue(p.categoriaNombre());
                row.createCell(2).setCellValue(p.nombre());
                texto(row, 3, p.descripcion());
                texto(row, 4, p.presentacion());
                row.createCell(5).setCellValue(p.unidadVenta());
                row.createCell(6).setCellValue(p.precio().doubleValue());
                if (p.precioOferta() != null) row.createCell(7).setCellValue(p.precioOferta().doubleValue());
                if (p.ofertaDesde() != null) row.createCell(8).setCellValue(FECHA.format(p.ofertaDesde()));
                if (p.ofertaHasta() != null) row.createCell(9).setCellValue(FECHA.format(p.ofertaHasta()));
                row.createCell(10).setCellValue(p.disponible() ? "SI" : "NO");
                row.createCell(11).setCellValue(p.destacado() ? "SI" : "NO");
            }

            // Listas desplegables para no equivocarse escribiendo.
            DataValidationHelper dv = hoja.getDataValidationHelper();
            lista(hoja, dv, 5, UNIDADES.toArray(String[]::new));
            lista(hoja, dv, 10, new String[]{"SI", "NO"});
            lista(hoja, dv, 11, new String[]{"SI", "NO"});

            Sheet ayuda = wb.createSheet("Instrucciones");
            ayuda.setColumnWidth(0, 110 * 256);
            String[] lineas = {
                    "Cómo llenar la hoja Productos",
                    "",
                    "• Una fila por producto. No cambies los títulos de la primera fila.",
                    "• Las columnas con * son obligatorias: codigo, categoria, nombre, unidad_venta y precio.",
                    "• codigo: único en tu catálogo (ej. QUE-001). Si ya existe, el producto se actualiza; si no, se crea.",
                    "  Las fotos del ZIP se asignan por este código: QUE-001.jpg va al producto QUE-001.",
                    "• categoria: si no existe, se crea.",
                    "• unidad_venta: UNIDAD, LIBRA, KILO, GRAMOS, PAQUETE o CAJA.",
                    "• precio y precio_oferta: en pesos, sin puntos ni decimales (ej. 12500). La oferta debe ser menor que el precio.",
                    "• oferta_desde y oferta_hasta: fechas dd/mm/aaaa (opcionales). Sin fechas, la oferta queda activa.",
                    "• disponible y destacado: SI o NO. Si se dejan vacías: disponible SI, destacado NO.",
                    "• Las fotos no van en el Excel: súbelas aparte, una por una o en un ZIP.",
                    "",
                    "Si el archivo tiene errores, no se guarda nada: te mostramos la fila y el motivo para que lo corrijas y lo vuelvas a subir."
            };
            for (int i = 0; i < lineas.length; i++) ayuda.createRow(i).createCell(0).setCellValue(lineas[i]);
            ayuda.getRow(0).getCell(0).setCellStyle(titulo);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el Excel", e);
        }
    }

    private static void texto(Row row, int col, String valor) {
        if (valor != null) row.createCell(col).setCellValue(valor);
    }

    private static void lista(Sheet hoja, DataValidationHelper dv, int col, String[] valores) {
        DataValidation v = dv.createValidation(dv.createExplicitListConstraint(valores),
                new CellRangeAddressList(1, MAX_FILAS, col, col));
        v.setShowErrorBox(true);
        hoja.addValidationData(v);
    }
}
