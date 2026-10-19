package com.agora.catalog.catalogopublico;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Catálogo público por hash: precios ocultos, ofertas, agotados y empresas inactivas. */
@SpringBootTest(properties = "settings.security.rate-limit-enabled=false")
@AutoConfigureMockMvc
@Transactional
class CatalogoPublicoIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    private final String hash = "p" + UUID.randomUUID().toString().substring(1, 8);
    private long empresaId;
    private long quesos;
    private long ofertaId;
    private long agotadoId;

    @BeforeEach
    void datos() {
        empresaId = jdbc.queryForObject("INSERT INTO main.empresa (nombre, slug, public_hash) VALUES (?, ?, ?) RETURNING id",
                Long.class, "Quesera " + hash, "quesera-" + hash, hash);
        quesos = categoria("Quesos", true);
        long oculta = categoria("Oculta", false);
        ofertaId = producto(quesos, "Q-1", 10_000, 7_500, "current_date - 1", "current_date + 5", true);
        // Otras presentaciones: usan las fechas de oferta del producto.
        jdbc.update("""
                INSERT INTO main.producto_presentacion (empresa_id, producto_id, nombre, precio, precio_oferta, orden)
                VALUES (?, ?, 'Media libra', 5200, 4000, 1), (?, ?, 'Tajado', 3000, NULL, 2)
                """, empresaId, ofertaId, empresaId, ofertaId);
        producto(quesos, "Q-2", 8_000, 6_000, "current_date + 3", "current_date + 9", true);
        agotadoId = producto(quesos, "Q-3", 5_000, null, "NULL", "NULL", false);
        producto(oculta, "O-1", 1_000, null, "NULL", "NULL", true);

        long plan = jdbc.queryForObject("INSERT INTO main.coleccion (empresa_id, nombre) VALUES (?, 'Picada') RETURNING id",
                Long.class, empresaId);
        jdbc.update("INSERT INTO main.coleccion_producto (coleccion_id, producto_id) VALUES (?, ?)", plan, ofertaId);
        long soloAgotados = jdbc.queryForObject(
                "INSERT INTO main.coleccion (empresa_id, nombre) VALUES (?, 'Vacía') RETURNING id", Long.class, empresaId);
        jdbc.update("INSERT INTO main.coleccion_producto (coleccion_id, producto_id) VALUES (?, ?)", soloAgotados, agotadoId);
    }

    @Test
    void entregaElCatalogoConLaOfertaVigenteCalculadaYSinLaCategoriaInactiva() throws Exception {
        mvc.perform(get("/public/catalogos/" + hash))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=60")))
                .andExpect(jsonPath("$.negocio.nombre").value("Quesera " + hash))
                .andExpect(jsonPath("$.categorias", hasSize(1)))
                .andExpect(jsonPath("$.productos", hasSize(3)))
                .andExpect(jsonPath("$.productos[0].ofertaVigente").value(true))
                .andExpect(jsonPath("$.productos[0].precioFinal").value(7500))
                .andExpect(jsonPath("$.productos[0].descuento").value(25))
                .andExpect(jsonPath("$.productos[0].nuevo").value(true))
                .andExpect(jsonPath("$.productos[0].presentaciones", hasSize(2)))
                .andExpect(jsonPath("$.productos[0].presentaciones[0].nombre").value("Media libra"))
                .andExpect(jsonPath("$.productos[0].presentaciones[0].precioFinal").value(4000))
                .andExpect(jsonPath("$.productos[0].presentaciones[0].descuento").value(23))
                .andExpect(jsonPath("$.productos[0].presentaciones[1].ofertaVigente").value(false))
                .andExpect(jsonPath("$.productos[0].presentaciones[1].precioFinal").value(3000))
                .andExpect(jsonPath("$.productos[1].presentaciones", hasSize(0)))
                // La oferta del Q-2 todavía no empieza: precio normal y sin precio de oferta.
                .andExpect(jsonPath("$.productos[1].ofertaVigente").value(false))
                .andExpect(jsonPath("$.productos[1].precioFinal").value(8000))
                .andExpect(jsonPath("$.productos[1].precioOferta").doesNotExist())
                .andExpect(jsonPath("$.productos[2].disponible").value(false));
    }

    @Test
    void conLosPreciosOcultosNoViajaNingunPrecioNiOferta() throws Exception {
        jdbc.update("UPDATE main.empresa SET mostrar_precios = FALSE WHERE id = ?", empresaId);

        mvc.perform(get("/public/catalogos/" + hash))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.negocio.mostrarPrecios").value(false))
                .andExpect(jsonPath("$.productos[0].precio").doesNotExist())
                .andExpect(jsonPath("$.productos[0].precioOferta").doesNotExist())
                .andExpect(jsonPath("$.productos[0].precioFinal").doesNotExist())
                .andExpect(jsonPath("$.productos[0].ofertaVigente").doesNotExist())
                .andExpect(jsonPath("$.productos[0].descuento").doesNotExist())
                .andExpect(jsonPath("$.productos[0].presentaciones[0].nombre").value("Media libra"))
                .andExpect(jsonPath("$.productos[0].presentaciones[0].precio").doesNotExist())
                .andExpect(jsonPath("$.productos[0].presentaciones[0].precioFinal").doesNotExist())
                .andExpect(jsonPath("$.productos[0].presentaciones[0].ofertaVigente").doesNotExist());
    }

    @Test
    void sinMostrarAgotadosSeExcluyenYLaColeccionQueQuedaVaciaNoSale() throws Exception {
        jdbc.update("UPDATE main.empresa SET mostrar_agotados = FALSE WHERE id = ?", empresaId);

        mvc.perform(get("/public/catalogos/" + hash))
                .andExpect(jsonPath("$.productos", hasSize(2)))
                .andExpect(jsonPath("$.colecciones", hasSize(1)))
                .andExpect(jsonPath("$.colecciones[0].nombre").value("Picada"))
                .andExpect(jsonPath("$.colecciones[0].productos[0]").value(ofertaId));
    }

    @Test
    void unaEmpresaInactivaOUnHashQueNoExisteDanElMismo404() throws Exception {
        jdbc.update("UPDATE main.empresa SET activa = FALSE WHERE id = ?", empresaId);

        mvc.perform(get("/public/catalogos/" + hash))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Catálogo no disponible"));
        mvc.perform(get("/public/catalogos/zzzzzzzz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Catálogo no disponible"));
    }

    private long categoria(String nombre, boolean activa) {
        return jdbc.queryForObject("INSERT INTO main.categoria (empresa_id, nombre, activa) VALUES (?, ?, ?) RETURNING id",
                Long.class, empresaId, nombre, activa);
    }

    private long producto(long categoriaId, String codigo, int precio, Integer oferta, String desde, String hasta,
                          boolean disponible) {
        return jdbc.queryForObject("""
                INSERT INTO main.producto (empresa_id, categoria_id, codigo, nombre, unidad_venta, precio, precio_oferta,
                                           oferta_desde, oferta_hasta, disponible, orden)
                VALUES (?, ?, ?, ?, 'UNIDAD', ?, ?, %s, %s, ?, ?) RETURNING id
                """.formatted(desde, hasta), Long.class, empresaId, categoriaId, codigo, "Producto " + codigo, precio,
                oferta, disponible, Integer.parseInt(codigo.substring(2)));
    }
}
