package com.agora.catalog.estadistica;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contadores del catálogo público y su lectura en el panel, cada empresa con lo suyo. */
@SpringBootTest(properties = "settings.security.rate-limit-enabled=false")
@AutoConfigureMockMvc
@Transactional
class EstadisticaIntegrationTest {

    private static final String CLAVE = "Clave1234";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 7);
    private String hashA;
    private String hashB;
    private long productoA;
    private long productoB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void datos() throws Exception {
        long a = empresa("a"), b = empresa("b");
        hashA = "a" + sufijo;
        hashB = "b" + sufijo;
        productoA = producto(a, "Queso campesino");
        productoB = producto(b, "Pan de bono");
        tokenA = admin(a, "a");
        tokenB = admin(b, "b");
    }

    @Test
    void cuentaVisitasProductosYPedidosSoloDeLaEmpresaDelLink() throws Exception {
        evento(hashA, "VISITA", null);
        evento(hashA, "VISITA", null);
        evento(hashA, "PRODUCTO", productoA);
        evento(hashA, "PRODUCTO", productoA);
        evento(hashA, "PRODUCTO", productoB); // producto de otra empresa: no cuenta
        evento(hashA, "PEDIDO", null);
        evento(hashA, "WHATSAPP", null);

        mvc.perform(get("/admin/estadisticas").param("dias", "7").header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dias").value(7))
                .andExpect(jsonPath("$.porDia.length()").value(7))
                .andExpect(jsonPath("$.visitas").value(2))
                .andExpect(jsonPath("$.pedidos").value(1))
                .andExpect(jsonPath("$.whatsapp").value(1))
                .andExpect(jsonPath("$.porDia[6].productosVistos").value(2))
                .andExpect(jsonPath("$.masVistos.length()").value(1))
                .andExpect(jsonPath("$.masVistos[0].nombre").value("Queso campesino"))
                .andExpect(jsonPath("$.masVistos[0].vistas").value(2));

        // La otra empresa no ve nada de esto.
        mvc.perform(get("/admin/estadisticas").header("Authorization", tokenB))
                .andExpect(jsonPath("$.visitas").value(0))
                .andExpect(jsonPath("$.masVistos.length()").value(0));
    }

    @Test
    void unCatalogoDesactivadoOInexistenteNoCuentaYNoDaError() throws Exception {
        jdbc.update("UPDATE main.empresa SET activa = FALSE WHERE public_hash = ?", hashB);
        evento(hashB, "VISITA", null);
        evento("noexiste", "VISITA", null);
        jdbc.update("UPDATE main.empresa SET activa = TRUE WHERE public_hash = ?", hashB);
        mvc.perform(get("/admin/estadisticas").header("Authorization", tokenB)).andExpect(jsonPath("$.visitas").value(0));
    }

    @Test
    void unTipoDesconocidoSeRechaza() throws Exception {
        mvc.perform(post("/public/catalogos/" + hashA + "/eventos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"COMPRA\"}")).andExpect(status().isBadRequest());
    }

    private void evento(String hash, String tipo, Long productoId) throws Exception {
        mvc.perform(post("/public/catalogos/" + hash + "/eventos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"" + tipo + "\",\"productoId\":" + productoId + "}"))
                .andExpect(status().isNoContent());
    }

    private long empresa(String letra) {
        return jdbc.queryForObject("INSERT INTO main.empresa (nombre, slug, public_hash) VALUES (?, ?, ?) RETURNING id",
                Long.class, "Empresa " + letra, "empresa-" + letra + "-" + sufijo, letra + sufijo);
    }

    private long producto(long empresa, String nombre) {
        long cat = jdbc.queryForObject("INSERT INTO main.categoria (empresa_id, nombre) VALUES (?, 'General') RETURNING id",
                Long.class, empresa);
        return jdbc.queryForObject("""
                INSERT INTO main.producto (empresa_id, categoria_id, codigo, nombre, unidad_venta, precio)
                VALUES (?, ?, 'P-1', ?, 'UNIDAD', 1000) RETURNING id
                """, Long.class, empresa, cat, nombre);
    }

    private String admin(long empresa, String letra) throws Exception {
        String email = "est-" + letra + "-" + sufijo + "@prueba.local";
        jdbc.update("INSERT INTO main.usuario (empresa_id, nombre, email, password_hash, rol) VALUES (?, 'P', ?, ?, 'ADMIN_EMPRESA')",
                empresa, email, passwordEncoder.encode(CLAVE));
        String sesion = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + CLAVE + "\"}")).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(sesion, "$.accessToken");
    }
}
