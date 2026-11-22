package com.agora.catalog.carro.repository;

import com.agora.catalog.carro.dto.CarroDtos.CarroResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Toda consulta filtra por empresa: un carro de otra empresa no existe para quien pregunta. */
@Repository
public class CarroRepository {

    /**
     * Precio de catálogo de lo que hay en el carro: el de la presentación si la línea la tiene, si no
     * el del producto. La oferta no cuenta: es un valor de referencia, no lo que se va a cobrar.
     */
    static final String PRECIO_STOCK = """
            COALESCE((SELECT pp.precio FROM main.producto_presentacion pp
                       WHERE pp.producto_id = s.producto_id AND lower(pp.nombre) = lower(s.presentacion)), p.precio)
            """;

    private static final String SELECT = """
            SELECT c.id, c.nombre, c.placa, c.activo,
                   (SELECT count(*) FROM main.carro_stock s WHERE s.carro_id = c.id) AS productos,
                   (SELECT COALESCE(round(sum(s.cantidad * %s)), 0)
                      FROM main.carro_stock s JOIN main.producto p ON p.id = s.producto_id
                     WHERE s.carro_id = c.id) AS valor
              FROM main.carro c
            """.formatted(PRECIO_STOCK);

    private static final RowMapper<CarroResponse> MAPPER = (rs, i) -> new CarroResponse(
            rs.getLong("id"), rs.getString("nombre"), rs.getString("placa"), rs.getBoolean("activo"),
            rs.getInt("productos"), rs.getLong("valor"));

    private final JdbcTemplate jdbc;

    public CarroRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CarroResponse> findAll(long empresaId) {
        return jdbc.query(SELECT + " WHERE c.empresa_id = ? AND c.eliminado_at IS NULL ORDER BY c.activo DESC, lower(c.nombre), c.id", MAPPER, empresaId);
    }

    public Optional<CarroResponse> findById(long empresaId, long id) {
        return jdbc.query(SELECT + " WHERE c.empresa_id = ? AND c.id = ? AND c.eliminado_at IS NULL", MAPPER, empresaId, id).stream().findFirst();
    }

    /**
     * Bloquea el carro hasta el final de la transacción: dos cargas, ajustes o ventas sobre el
     * mismo carro se hacen una detrás de la otra y el stock no se pisa. Devuelve si está activo.
     */
    public Optional<Boolean> lock(long empresaId, long id) {
        return jdbc.query("SELECT activo FROM main.carro WHERE empresa_id = ? AND id = ? AND eliminado_at IS NULL FOR UPDATE",
                (rs, i) -> rs.getBoolean("activo"), empresaId, id).stream().findFirst();
    }

    public boolean existsByNombre(long empresaId, String nombre, Long exceptId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM main.carro
                                WHERE empresa_id = ? AND lower(nombre) = lower(?) AND id IS DISTINCT FROM ? AND eliminado_at IS NULL)
                """, Boolean.class, empresaId, nombre, exceptId));
    }

    public long insert(long empresaId, String nombre, String placa, boolean activo) {
        return jdbc.queryForObject("INSERT INTO main.carro (empresa_id, nombre, placa, activo) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class, empresaId, nombre, placa, activo);
    }

    public int update(long empresaId, long id, String nombre, String placa, boolean activo) {
        return jdbc.update("UPDATE main.carro SET nombre = ?, placa = ?, activo = ?, updated_at = now() WHERE empresa_id = ? AND id = ? AND eliminado_at IS NULL",
                nombre, placa, activo, empresaId, id);
    }

    public int setActivo(long empresaId, long id, boolean activo) {
        return jdbc.update("UPDATE main.carro SET activo = ?, updated_at = now() WHERE empresa_id = ? AND id = ? AND eliminado_at IS NULL",
                activo, empresaId, id);
    }

    /** Movimientos y pedidos que tiene: si tiene alguno, no se borra (se perdería el historial). */
    public int historial(long empresaId, long id) {
        return jdbc.queryForObject("""
                SELECT (SELECT count(*) FROM main.carro_movimiento WHERE empresa_id = ? AND carro_id = ?)
                     + (SELECT count(*) FROM main.pedido_tienda WHERE empresa_id = ? AND carro_id = ?)
                """, Integer.class, empresaId, id, empresaId, id);
    }

    public int delete(long empresaId, long id) {
        return jdbc.update("DELETE FROM main.carro WHERE empresa_id = ? AND id = ?", empresaId, id);
    }

    /** Un carro con historial no se borra: se marca y desaparece del panel; sus pedidos lo siguen nombrando. */
    public int marcarEliminado(long empresaId, long id) {
        // La ruta que iba termina con el carro: no sigue «en curso» para siempre.
        jdbc.update("UPDATE main.carro_ruta SET cerrada_at = now() WHERE empresa_id = ? AND carro_id = ? AND cerrada_at IS NULL",
                empresaId, id);
        return jdbc.update("UPDATE main.carro SET eliminado_at = now(), activo = FALSE, updated_at = now() WHERE empresa_id = ? AND id = ?",
                empresaId, id);
    }
}
