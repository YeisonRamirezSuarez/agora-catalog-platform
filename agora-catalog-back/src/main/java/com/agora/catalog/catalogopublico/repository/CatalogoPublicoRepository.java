package com.agora.catalog.catalogopublico.repository;

import com.agora.catalog.catalogopublico.dto.response.CatalogoPublicoResponse.Categoria;
import com.agora.catalog.catalogopublico.dto.response.CatalogoPublicoResponse.Franja;
import com.agora.catalog.catalogopublico.dto.response.CatalogoPublicoResponse.Horario;
import com.agora.catalog.catalogopublico.dto.response.CatalogoPublicoResponse.Negocio;
import com.agora.catalog.catalogopublico.dto.response.CatalogoPublicoResponse.Sello;
import com.agora.catalog.catalogopublico.dto.response.CatalogoPublicoResponse.Whatsapp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** Lecturas del catálogo público. Todo filtra por empresa y por lo que el negocio dejó visible. */
@Repository
public class CatalogoPublicoRepository {

    /** Empresa activa encontrada por su hash, con lo que el servicio necesita para decidir. */
    public record EmpresaPublica(long id, Negocio negocio, boolean mostrarAgotados) {
    }

    /** Producto tal como está en la base; el servicio calcula oferta, precio final y "nuevo". */
    public record ProductoFila(long id, long categoriaId, String nombre, String descripcion, String presentacion,
                               String unidadVenta, BigDecimal precio, BigDecimal precioOferta, LocalDate ofertaDesde,
                               LocalDate ofertaHasta, String imagenUrl, String imagenThumbUrl, boolean disponible,
                               boolean destacado, Instant creadoEn) {
    }

    public record ColeccionFila(long id, String nombre, String descripcion, String imagenUrl, Long productoId) {
    }

    private final JdbcTemplate jdbc;

    public CatalogoPublicoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<EmpresaPublica> findEmpresaActiva(String publicHash) {
        return jdbc.query("SELECT * FROM main.empresa WHERE public_hash = ? AND activa", (rs, i) -> new EmpresaPublica(
                rs.getLong("id"),
                new Negocio(rs.getString("nombre"), rs.getString("nombre_corto"), rs.getString("lema"),
                        rs.getString("slug"), rs.getString("public_hash"), rs.getString("logo_url"),
                        rs.getString("portada_url"), rs.getString("color_primario"), rs.getString("color_secundario"),
                        rs.getString("portada_antetitulo"), rs.getString("portada_titulo"),
                        rs.getString("portada_titulo_acento"), rs.getString("portada_subtitulo"),
                        rs.getString("portada_nota"), rs.getString("portada_sello_texto"),
                        rs.getString("catalogo_titulo"), rs.getString("catalogo_acento"),
                        rs.getString("catalogo_subtitulo"), rs.getString("colecciones_titulo"),
                        rs.getString("colecciones_texto"), rs.getString("colecciones_nota"),
                        rs.getString("visita_nota"), rs.getString("fachada"), rs.getBoolean("adornos"),
                        rs.getString("direccion"), rs.getString("horario"), rs.getString("info_domicilios"),
                        rs.getBoolean("domicilio_activo"), rs.getBigDecimal("domicilio_costo"),
                        rs.getBigDecimal("domicilio_minimo"), rs.getBigDecimal("domicilio_gratis_desde"),
                        rs.getBoolean("mostrar_precios"), rs.getBoolean("pedidos_whatsapp")),
                rs.getBoolean("mostrar_agotados")), publicHash).stream().findFirst();
    }

    public List<Whatsapp> findWhatsapps(long empresaId) {
        return jdbc.query("SELECT numero, nombre FROM main.empresa_whatsapp WHERE empresa_id = ? ORDER BY orden, id",
                (rs, i) -> new Whatsapp(rs.getString("numero"), rs.getString("nombre")), empresaId);
    }

    public List<Sello> findSellos(long empresaId) {
        return jdbc.query("SELECT icono, texto FROM main.empresa_sello WHERE empresa_id = ? ORDER BY orden, id",
                (rs, i) -> new Sello(rs.getString("icono"), rs.getString("texto")), empresaId);
    }

    public List<Horario> findHorario(long empresaId) {
        return jdbc.query("""
                SELECT dia, to_char(abre, 'HH24:MI') AS abre, to_char(cierra, 'HH24:MI') AS cierra
                  FROM main.empresa_horario WHERE empresa_id = ? ORDER BY dia
                """, (rs, i) -> new Horario(rs.getInt("dia"), rs.getString("abre"), rs.getString("cierra")), empresaId);
    }

    public List<Categoria> findCategoriasActivas(long empresaId) {
        return jdbc.query("""
                SELECT id, nombre, imagen_url FROM main.categoria
                 WHERE empresa_id = ? AND activa ORDER BY orden, lower(nombre)
                """, (rs, i) -> new Categoria(rs.getLong("id"), rs.getString("nombre"), rs.getString("imagen_url")),
                empresaId);
    }

    public Optional<Franja> findFranja(long empresaId) {
        return jdbc.query("""
                SELECT id, banner_titulo, banner_frase, banner_nota, banner_url FROM main.categoria
                 WHERE empresa_id = ? AND activa AND banner_activo
                """, (rs, i) -> new Franja(rs.getLong("id"), rs.getString("banner_titulo"), rs.getString("banner_frase"),
                rs.getString("banner_nota"), rs.getString("banner_url")), empresaId).stream().findFirst();
    }

    /** Productos de categorías activas; los agotados solo si el negocio los muestra. */
    private static final RowMapper<ProductoFila> PRODUCTO = (rs, i) -> new ProductoFila(rs.getLong("id"),
            rs.getLong("categoria_id"), rs.getString("nombre"), rs.getString("descripcion"), rs.getString("presentacion"),
            rs.getString("unidad_venta"), rs.getBigDecimal("precio"), rs.getBigDecimal("precio_oferta"),
            rs.getObject("oferta_desde", LocalDate.class), rs.getObject("oferta_hasta", LocalDate.class),
            rs.getString("imagen_url"), rs.getString("imagen_thumb_url"), rs.getBoolean("disponible"),
            rs.getBoolean("destacado"), rs.getObject("created_at", OffsetDateTime.class).toInstant());

    public List<ProductoFila> findProductos(long empresaId, boolean incluirAgotados) {
        return jdbc.query("""
                SELECT p.* FROM main.producto p
                  JOIN main.categoria c ON c.id = p.categoria_id AND c.activa
                 WHERE p.empresa_id = ? AND (p.disponible OR ?)
                 ORDER BY c.orden, p.orden, lower(p.nombre)
                """, PRODUCTO, empresaId, incluirAgotados);
    }

    /** Un producto tal como lo ve el catálogo público (categoría activa; agotado solo si se muestran). */
    public Optional<ProductoFila> findProducto(long empresaId, long id, boolean incluirAgotados) {
        return jdbc.query("""
                SELECT p.* FROM main.producto p
                  JOIN main.categoria c ON c.id = p.categoria_id AND c.activa
                 WHERE p.empresa_id = ? AND p.id = ? AND (p.disponible OR ?)
                """, PRODUCTO, empresaId, id, incluirAgotados).stream().findFirst();
    }

    /** Una fila por colección visible y producto (productoId null si no tiene ninguno). */
    public List<ColeccionFila> findColeccionesVisibles(long empresaId) {
        return jdbc.query("""
                SELECT k.id, k.nombre, k.descripcion, k.imagen_url, cp.producto_id
                  FROM main.coleccion k
                  LEFT JOIN main.coleccion_producto cp ON cp.coleccion_id = k.id
                 WHERE k.empresa_id = ? AND k.visible
                 ORDER BY k.orden, k.id, cp.orden
                """, (rs, i) -> new ColeccionFila(rs.getLong("id"), rs.getString("nombre"), rs.getString("descripcion"),
                rs.getString("imagen_url"), rs.getObject("producto_id", Long.class)), empresaId);
    }
}
