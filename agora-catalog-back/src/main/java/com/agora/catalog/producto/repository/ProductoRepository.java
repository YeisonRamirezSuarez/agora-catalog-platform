package com.agora.catalog.producto.repository;

import com.agora.catalog.producto.dto.ProductoDtos.ProductoRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Toda consulta filtra por empresa: un producto de otra empresa no existe para quien pregunta. */
@Repository
public class ProductoRepository {

    /** Fila de producto; el servicio calcula la oferta vigente con su reloj. */
    public record Fila(long id, String codigo, long categoriaId, String categoriaNombre, String nombre, String descripcion,
                       String presentacion, String unidadVenta, BigDecimal precio, BigDecimal precioOferta,
                       LocalDate ofertaDesde, LocalDate ofertaHasta, String imagenUrl, String imagenThumbUrl,
                       boolean disponible, boolean destacado, int orden) {
    }

    /** Filtros del listado del panel. */
    public record Filtro(String q, Long categoriaId, Boolean disponible) {
    }

    private static final String SELECT = """
            SELECT p.*, c.nombre AS categoria_nombre
              FROM main.producto p JOIN main.categoria c ON c.id = p.categoria_id
            """;

    private static final RowMapper<Fila> MAPPER = (rs, i) -> new Fila(
            rs.getLong("id"), rs.getString("codigo"), rs.getLong("categoria_id"), rs.getString("categoria_nombre"),
            rs.getString("nombre"), rs.getString("descripcion"), rs.getString("presentacion"), rs.getString("unidad_venta"),
            rs.getBigDecimal("precio"), rs.getBigDecimal("precio_oferta"),
            rs.getObject("oferta_desde", LocalDate.class), rs.getObject("oferta_hasta", LocalDate.class),
            rs.getString("imagen_url"), rs.getString("imagen_thumb_url"), rs.getBoolean("disponible"),
            rs.getBoolean("destacado"), rs.getInt("orden"));

    private final JdbcTemplate jdbc;

    public ProductoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Fila> findById(long empresaId, long id) {
        return jdbc.query(SELECT + " WHERE p.empresa_id = ? AND p.id = ?", MAPPER, empresaId, id).stream().findFirst();
    }

    public List<Fila> page(long empresaId, Filtro f, int page, int size) {
        List<Object> args = new ArrayList<>();
        String where = where(empresaId, f, args);
        args.add(size);
        args.add((long) page * size);
        return jdbc.query(SELECT + where + " ORDER BY c.orden, p.orden, lower(p.nombre), p.id LIMIT ? OFFSET ?",
                MAPPER, args.toArray());
    }

    public int count(long empresaId, Filtro f) {
        List<Object> args = new ArrayList<>();
        String where = where(empresaId, f, args);
        return jdbc.queryForObject("SELECT count(*) FROM main.producto p JOIN main.categoria c ON c.id = p.categoria_id"
                + where, Integer.class, args.toArray());
    }

    private static String where(long empresaId, Filtro f, List<Object> args) {
        StringBuilder w = new StringBuilder(" WHERE p.empresa_id = ?");
        args.add(empresaId);
        if (f.q() != null && !f.q().isBlank()) {
            // Sin tildes ni mayúsculas, en nombre y código.
            w.append(" AND (translate(lower(p.nombre), 'áéíóúüñ', 'aeiouun') LIKE translate(lower(?), 'áéíóúüñ', 'aeiouun')"
                    + " OR lower(p.codigo) LIKE lower(?))");
            String like = "%" + f.q().trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            args.add(like);
            args.add(like);
        }
        if (f.categoriaId() != null) {
            w.append(" AND p.categoria_id = ?");
            args.add(f.categoriaId());
        }
        if (f.disponible() != null) {
            w.append(" AND p.disponible = ?");
            args.add(f.disponible());
        }
        return w.toString();
    }

    public boolean existsByCodigo(long empresaId, String codigo, Long exceptId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM main.producto
                                WHERE empresa_id = ? AND lower(codigo) = lower(?) AND id IS DISTINCT FROM ?)
                """, Boolean.class, empresaId, codigo, exceptId));
    }

    public long insert(long empresaId, ProductoRequest r) {
        return jdbc.queryForObject("""
                INSERT INTO main.producto (empresa_id, categoria_id, codigo, nombre, descripcion, presentacion, unidad_venta,
                                           precio, precio_oferta, oferta_desde, oferta_hasta, disponible, destacado, orden)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                        (SELECT COALESCE(MAX(orden), 0) + 1 FROM main.producto WHERE empresa_id = ? AND categoria_id = ?))
                RETURNING id
                """, Long.class, empresaId, r.categoriaId(), r.codigo().trim(), r.nombre().trim(), blank(r.descripcion()),
                blank(r.presentacion()), r.unidadVenta(), r.precio(), r.precioOferta(), r.ofertaDesde(), r.ofertaHasta(),
                r.disponible(), r.destacado(), empresaId, r.categoriaId());
    }

    public int update(long empresaId, long id, ProductoRequest r) {
        return jdbc.update("""
                UPDATE main.producto SET categoria_id = ?, codigo = ?, nombre = ?, descripcion = ?, presentacion = ?,
                       unidad_venta = ?, precio = ?, precio_oferta = ?, oferta_desde = ?, oferta_hasta = ?,
                       disponible = ?, destacado = ?, updated_at = now()
                 WHERE empresa_id = ? AND id = ?
                """, r.categoriaId(), r.codigo().trim(), r.nombre().trim(), blank(r.descripcion()), blank(r.presentacion()),
                r.unidadVenta(), r.precio(), r.precioOferta(), r.ofertaDesde(), r.ofertaHasta(), r.disponible(),
                r.destacado(), empresaId, id);
    }

    public int delete(long empresaId, long id) {
        return jdbc.update("DELETE FROM main.producto WHERE empresa_id = ? AND id = ?", empresaId, id);
    }

    public int setDisponible(long empresaId, long id, boolean disponible) {
        return jdbc.update("UPDATE main.producto SET disponible = ?, updated_at = now() WHERE empresa_id = ? AND id = ?",
                disponible, empresaId, id);
    }

    public int setPrecio(long empresaId, long id, long precio) {
        return jdbc.update("UPDATE main.producto SET precio = ?, updated_at = now() WHERE empresa_id = ? AND id = ?",
                precio, empresaId, id);
    }

    public void setImagen(long empresaId, long id, String url, String thumb) {
        jdbc.update("UPDATE main.producto SET imagen_url = ?, imagen_thumb_url = ?, updated_at = now() WHERE empresa_id = ? AND id = ?",
                url, thumb, empresaId, id);
    }

    public boolean categoriaDeLaEmpresa(long empresaId, long categoriaId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM main.categoria WHERE empresa_id = ? AND id = ?)", Boolean.class,
                empresaId, categoriaId));
    }

    /** Contadores del inicio del panel; la oferta vigente se cuenta con la fecha del reloj del servicio. */
    public int[] contadores(long empresaId, LocalDate hoy) {
        return jdbc.queryForObject("""
                SELECT count(*),
                       count(*) FILTER (WHERE imagen_url IS NULL),
                       count(*) FILTER (WHERE NOT disponible),
                       count(*) FILTER (WHERE precio_oferta IS NOT NULL AND (oferta_desde IS NULL OR oferta_desde <= ?)
                                          AND (oferta_hasta IS NULL OR oferta_hasta >= ?))
                  FROM main.producto WHERE empresa_id = ?
                """, (rs, i) -> new int[]{rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4)}, hoy, hoy, empresaId);
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
