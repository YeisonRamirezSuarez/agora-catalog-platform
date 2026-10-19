package com.agora.catalog.importacion.service;

import com.agora.catalog.categoria.dto.CategoriaDtos.CategoriaResponse;
import com.agora.catalog.categoria.repository.CategoriaRepository;
import com.agora.catalog.importacion.dto.ImportacionDtos.ErrorFila;
import com.agora.catalog.importacion.dto.ImportacionDtos.ReporteImportacion;
import com.agora.catalog.producto.dto.ProductoDtos.ProductoRequest;
import com.agora.catalog.producto.repository.ProductoRepository;
import com.agora.catalog.producto.service.ValidarProducto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Productos desde Excel (ESPECIFICACION.md §6). Se revisa todo el archivo antes de guardar: con un
 * solo error no se guarda nada, para no dejar el catálogo a medio cargar.
 */
@Service
public class ImportarProductosService {

    public static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final DateTimeFormatter[] FECHAS = {
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ISO_LOCAL_DATE
    };

    private final ProductoRepository productos;
    private final CategoriaRepository categorias;
    private final Validator validator;

    public ImportarProductosService(ProductoRepository productos, CategoriaRepository categorias, Validator validator) {
        this.productos = productos;
        this.categorias = categorias;
        this.validator = validator;
    }

    public byte[] exportar(long empresaId) {
        return ExcelProductos.exportar(productos.findAll(empresaId));
    }

    /** Revisa el archivo sin guardar nada. */
    public ReporteImportacion previsualizar(long empresaId, byte[] archivo) {
        return revisar(empresaId, archivo).reporte(false);
    }

    /** Guarda todo o nada: con errores devuelve el mismo reporte de la previsualización, sin aplicar. */
    @Transactional
    public ReporteImportacion confirmar(long empresaId, byte[] archivo) {
        Revision rev = revisar(empresaId, archivo);
        if (!rev.errores.isEmpty()) return rev.reporte(false);

        Map<String, Long> categoriaIds = new HashMap<>(rev.categoriaIds);
        for (String nombre : rev.categoriasNuevas) {
            categoriaIds.put(clave(nombre), categorias.insert(empresaId, nombre, true));
        }
        for (FilaExcel f : rev.filas) {
            ProductoRequest r = conCategoria(f.producto, categoriaIds.get(clave(f.categoria)));
            Long id = rev.existentes.get(clave(r.codigo()));
            if (id == null) productos.insert(empresaId, r);
            else productos.update(empresaId, id, r);
        }
        return rev.reporte(true);
    }

    // ---------- Revisión ----------

    private record FilaExcel(int fila, String categoria, ProductoRequest producto) {
    }

    private static final class Revision {
        final List<FilaExcel> filas = new ArrayList<>();
        final List<ErrorFila> errores = new ArrayList<>();
        final List<String> categoriasNuevas = new ArrayList<>();
        Map<String, Long> categoriaIds = Map.of();
        Map<String, Long> existentes = Map.of();
        int leidas;

        ReporteImportacion reporte(boolean aplicado) {
            long nuevos = filas.stream().filter(f -> !existentes.containsKey(clave(f.producto.codigo()))).count();
            return new ReporteImportacion(leidas, (int) nuevos, filas.size() - (int) nuevos, List.copyOf(categoriasNuevas), List.copyOf(errores), aplicado);
        }
    }

    private Revision revisar(long empresaId, byte[] archivo) {
        if (archivo == null || archivo.length == 0) throw new IllegalArgumentException("Elige el archivo de Excel");
        if (archivo.length > MAX_BYTES) throw new IllegalArgumentException("El Excel pesa más de 5 MB");

        Revision rev = new Revision();
        rev.categoriaIds = new HashMap<>();
        for (CategoriaResponse c : categorias.findAll(empresaId)) rev.categoriaIds.put(clave(c.nombre()), c.id());
        rev.existentes = new HashMap<>();
        for (ProductoRepository.Fila p : productos.findAll(empresaId)) rev.existentes.put(clave(p.codigo()), p.id());

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            Sheet hoja = wb.getSheet(ExcelProductos.HOJA) != null ? wb.getSheet(ExcelProductos.HOJA) : wb.getSheetAt(0);
            Map<String, Integer> columnas = columnas(hoja.getRow(0));
            for (String c : ExcelProductos.OBLIGATORIAS) {
                if (!columnas.containsKey(c)) rev.errores.add(new ErrorFila(1, "Falta la columna «" + c + "»"));
            }
            if (!rev.errores.isEmpty()) return rev;

            Map<String, Integer> codigos = new HashMap<>();
            Map<String, String> nuevas = new LinkedHashMap<>();
            int contadas = 0;
            for (int i = 1; i <= hoja.getLastRowNum(); i++) {
                Row row = hoja.getRow(i);
                if (vacia(row, columnas)) continue;
                if (++contadas > ExcelProductos.MAX_FILAS) {
                    rev.errores.add(new ErrorFila(i + 1, "El archivo tiene más de " + ExcelProductos.MAX_FILAS
                            + " productos. Divídelo en varios archivos."));
                    break;
                }
                List<String> errores = new ArrayList<>();
                FilaExcel f = leerFila(row, i + 1, columnas, errores);
                String codigo = clave(f.producto.codigo());
                if (!codigo.isEmpty()) {
                    Integer otra = codigos.putIfAbsent(codigo, f.fila);
                    if (otra != null) errores.add("El código " + f.producto.codigo() + " ya está en la fila " + otra);
                }
                if (errores.isEmpty()) {
                    rev.filas.add(f);
                    if (!rev.categoriaIds.containsKey(clave(f.categoria))) nuevas.putIfAbsent(clave(f.categoria), f.categoria);
                } else {
                    rev.errores.add(new ErrorFila(f.fila, String.join(". ", errores)));
                }
            }
            if (contadas == 0 && rev.errores.isEmpty()) rev.errores.add(new ErrorFila(2, "El archivo no tiene productos"));
            rev.leidas = contadas;
            rev.categoriasNuevas.addAll(nuevas.values());
            return rev;
        } catch (Exception e) {
            // POI lanza de todo con un archivo que no es .xlsx (o viene dañado).
            throw new IllegalArgumentException("No pudimos leer el archivo. Usa la plantilla en formato .xlsx.", e);
        }
    }

    private FilaExcel leerFila(Row row, int fila, Map<String, Integer> col, List<String> errores) {
        String categoria = texto(row, col, "categoria");
        if (categoria == null) errores.add("Escribe la categoría");
        else if (categoria.length() > 60) errores.add("La categoría puede tener hasta 60 caracteres");

        String unidad = texto(row, col, "unidad_venta");
        ProductoRequest r = new ProductoRequest(
                texto(row, col, "codigo"),
                0L, // la categoría real se resuelve al guardar (puede ser nueva)
                texto(row, col, "nombre"),
                texto(row, col, "descripcion"),
                texto(row, col, "presentacion"),
                unidad == null ? null : unidad.toUpperCase(Locale.ROOT),
                precio(row, col, "precio", errores),
                precio(row, col, "precio_oferta", errores),
                fecha(row, col, "oferta_desde", errores),
                fecha(row, col, "oferta_hasta", errores),
                siNo(row, col, "disponible", true, errores),
                siNo(row, col, "destacado", false, errores));

        validator.validate(r).stream()
                .sorted((a, b) -> Integer.compare(orden(a), orden(b)))
                .map(ConstraintViolation::getMessage)
                .forEach(errores::add);
        errores.addAll(ValidarProducto.errores(r));
        return new FilaExcel(fila, categoria, r);
    }

    /** Los mensajes salen en el orden de las columnas del Excel. */
    private static int orden(ConstraintViolation<?> v) {
        String campo = v.getPropertyPath().toString().replaceAll("([A-Z])", "_$1").toLowerCase(Locale.ROOT);
        int i = ExcelProductos.COLUMNAS.indexOf(campo);
        return i < 0 ? Integer.MAX_VALUE : i;
    }

    // ---------- Celdas ----------

    private static final DataFormatter FORMATO = new DataFormatter(Locale.ROOT);

    private static Map<String, Integer> columnas(Row cabecera) {
        Map<String, Integer> m = new HashMap<>();
        if (cabecera == null) return m;
        for (Cell c : cabecera) {
            String t = FORMATO.formatCellValue(c).replace("*", "").trim().toLowerCase(Locale.ROOT).replace(' ', '_');
            if (!t.isEmpty()) m.putIfAbsent(t, c.getColumnIndex());
        }
        return m;
    }

    private static boolean vacia(Row row, Map<String, Integer> col) {
        if (row == null) return true;
        for (int i : col.values()) {
            Cell c = row.getCell(i);
            if (c != null && !FORMATO.formatCellValue(c).isBlank()) return false;
        }
        return true;
    }

    private static Cell celda(Row row, Map<String, Integer> col, String nombre) {
        Integer i = col.get(nombre);
        Cell c = i == null ? null : row.getCell(i);
        return c == null || tipo(c) == CellType.BLANK ? null : c;
    }

    private static CellType tipo(Cell c) {
        return c.getCellType() == CellType.FORMULA ? c.getCachedFormulaResultType() : c.getCellType();
    }

    /** Texto recortado; vacío es null. Un código numérico (1001) sale sin ".0". */
    private static String texto(Row row, Map<String, Integer> col, String nombre) {
        Cell c = celda(row, col, nombre);
        if (c == null) return null;
        String t = tipo(c) == CellType.STRING ? c.getStringCellValue() : FORMATO.formatCellValue(c);
        t = t.trim();
        return t.isEmpty() ? null : t;
    }

    /** Pesos sin decimales: 12500, "12500" o "$12.500". */
    static Long precio(Row row, Map<String, Integer> col, String nombre, List<String> errores) {
        Cell c = celda(row, col, nombre);
        if (c == null) return null;
        String etiqueta = nombre.equals("precio") ? "precio" : "precio de oferta";
        if (tipo(c) == CellType.NUMERIC) {
            double v = c.getNumericCellValue();
            if (v != Math.floor(v)) {
                errores.add("El " + etiqueta + " va sin decimales");
                return null;
            }
            return (long) v;
        }
        String t = texto(row, col, nombre);
        if (t == null) return null;
        String limpio = t.replace("$", "").replace(".", "").replace(" ", "");
        if (!limpio.matches("\\d{1,12}")) {
            errores.add("El " + etiqueta + " no es un número en pesos (ej. 12500)");
            return null;
        }
        return Long.parseLong(limpio);
    }

    private static LocalDate fecha(Row row, Map<String, Integer> col, String nombre, List<String> errores) {
        Cell c = celda(row, col, nombre);
        if (c == null) return null;
        if (tipo(c) == CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) {
            return DateUtil.getLocalDateTime(c.getNumericCellValue()).toLocalDate();
        }
        String t = texto(row, col, nombre);
        if (t == null) return null;
        for (DateTimeFormatter f : FECHAS) {
            try {
                return LocalDate.parse(t, f);
            } catch (DateTimeParseException ignored) {
                // se prueba el siguiente formato
            }
        }
        errores.add("La fecha de " + nombre.replace('_', ' ') + " no es válida: usa dd/mm/aaaa");
        return null;
    }

    private static Boolean siNo(Row row, Map<String, Integer> col, String nombre, boolean porDefecto, List<String> errores) {
        Cell c = celda(row, col, nombre);
        if (c == null) return porDefecto;
        if (tipo(c) == CellType.BOOLEAN) return c.getBooleanCellValue();
        String t = texto(row, col, nombre);
        if (t == null) return porDefecto;
        switch (t.toUpperCase(Locale.ROOT)) {
            case "SI", "SÍ", "S", "X" -> {
                return true;
            }
            case "NO", "N" -> {
                return false;
            }
            default -> {
                errores.add("En «" + nombre + "» escribe SI o NO");
                return porDefecto;
            }
        }
    }

    private static ProductoRequest conCategoria(ProductoRequest r, long categoriaId) {
        return new ProductoRequest(r.codigo(), categoriaId, r.nombre(), r.descripcion(), r.presentacion(), r.unidadVenta(),
                r.precio(), r.precioOferta(), r.ofertaDesde(), r.ofertaHasta(), r.disponible(), r.destacado());
    }

    static String clave(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
