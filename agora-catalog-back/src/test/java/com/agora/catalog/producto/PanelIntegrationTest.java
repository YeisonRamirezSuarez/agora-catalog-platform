package com.agora.catalog.producto;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Panel del negocio: categorías, productos, fotos y aislamiento entre empresas. */
@SpringBootTest(properties = {"settings.security.rate-limit-enabled=false",
        "settings.storage-path=${java.io.tmpdir}/agora-catalog-pruebas"})
@AutoConfigureMockMvc
@Transactional
class PanelIntegrationTest {

    private static final String CLAVE = "Clave1234";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 8);
    private String tokenA;
    private String tokenB;
    private long catA;
    private long catB;

    @BeforeEach
    void datos() throws Exception {
        tokenA = admin("a");
        tokenB = admin("b");
        catA = id(post("/admin/categorias").header("Authorization", tokenA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Quesos\",\"activa\":true}"));
        catB = id(post("/admin/categorias").header("Authorization", tokenB).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Quesos\",\"activa\":true}"));
    }

    @Test
    void otraEmpresaNoVeNiTocaUnProductoAjeno() throws Exception {
        long p = id(crearProducto(tokenA, catA, "Q-1", 10000, null));

        mvc.perform(get("/admin/productos/" + p).header("Authorization", tokenB)).andExpect(status().isNotFound());
        mvc.perform(put("/admin/productos/" + p).header("Authorization", tokenB).contentType(MediaType.APPLICATION_JSON)
                .content(producto(catB, "Q-1", 1, null))).andExpect(status().isNotFound());
        mvc.perform(patch("/admin/productos/" + p + "/disponible").header("Authorization", tokenB)
                .contentType(MediaType.APPLICATION_JSON).content("{\"disponible\":false}")).andExpect(status().isNotFound());
        mvc.perform(patch("/admin/productos/precios").header("Authorization", tokenB).contentType(MediaType.APPLICATION_JSON)
                .content("{\"cambios\":[{\"id\":" + p + ",\"precio\":1}]}")).andExpect(status().isNotFound());
        mvc.perform(delete("/admin/productos/" + p).header("Authorization", tokenB)).andExpect(status().isNotFound());
        mvc.perform(get("/admin/productos").header("Authorization", tokenB))
                .andExpect(jsonPath("$.totalElements").value(0));
        // Tampoco puede usar una categoría ajena en su producto.
        crearProducto(tokenB, catA, "B-1", 1000, null).andExpect(status().isNotFound());

        mvc.perform(get("/admin/productos/" + p).header("Authorization", tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.precio").value(10000));
    }

    @Test
    void lasReglasDelProductoSeValidan() throws Exception {
        crearProducto(tokenA, catA, "Q-1", 10000, 10000L).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("menor que el precio")));
        mvc.perform(post("/admin/productos").header("Authorization", tokenA).contentType(MediaType.APPLICATION_JSON)
                        .content(producto(catA, "Q-2", 10000, 8000L).replace("\"ofertaDesde\":null", "\"ofertaDesde\":\"2026-12-10\"")
                                .replace("\"ofertaHasta\":null", "\"ofertaHasta\":\"2026-12-01\"")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("fechas")));
        crearProducto(tokenA, catA, "Q-3", 10000, 8000L).andExpect(status().isCreated())
                .andExpect(jsonPath("$.ofertaVigente").value(true));
        crearProducto(tokenA, catA, "q-3", 5000, null).andExpect(status().isConflict());
    }

    @Test
    void otrasPresentacionesSeGuardanSeConservanSinListaYSeValidan() throws Exception {
        String con = producto(catA, "Q-1", 18000, null).replace("\"destacado\":false}", "\"destacado\":false,\"presentaciones\":"
                + "[{\"nombre\":\"Media libra\",\"precio\":9500,\"precioOferta\":9000},{\"nombre\":\"Tajado 250 g\",\"precio\":6000}]}");
        long p = id(post("/admin/productos").header("Authorization", tokenA).contentType(MediaType.APPLICATION_JSON).content(con));
        mvc.perform(get("/admin/productos/" + p).header("Authorization", tokenA))
                .andExpect(jsonPath("$.presentaciones", hasSize(2)))
                .andExpect(jsonPath("$.presentaciones[0].nombre").value("Media libra"))
                .andExpect(jsonPath("$.presentaciones[0].precioOferta").value(9000));
        mvc.perform(get("/admin/productos").header("Authorization", tokenA))
                .andExpect(jsonPath("$.content[0].presentaciones", hasSize(2)));

        // Sin la lista (como cada fila del Excel) se conservan; con la lista vacía se quitan.
        guardar(p, producto(catA, "Q-1", 18000, null)).andExpect(status().isOk()).andExpect(jsonPath("$.presentaciones", hasSize(2)));
        guardar(p, producto(catA, "Q-1", 18000, null).replace("\"destacado\":false}", "\"destacado\":false,\"presentaciones\":[]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.presentaciones", hasSize(0)));

        guardar(p, con.replace("Media libra", "bloque"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("repetida")));
        guardar(p, con.replace("\"precioOferta\":9000", "\"precioOferta\":9500"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("menor que su precio")));
        guardar(p, con.replace("\"presentacion\":\"Bloque\"", "\"presentacion\":null"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("principal")));
        // Otra empresa no le pone presentaciones a un producto ajeno.
        mvc.perform(put("/admin/productos/" + p).header("Authorization", tokenB).contentType(MediaType.APPLICATION_JSON)
                .content(con.replace("\"categoriaId\":" + catA, "\"categoriaId\":" + catB))).andExpect(status().isNotFound());
    }

    private ResultActions guardar(long id, String json) throws Exception {
        return mvc.perform(put("/admin/productos/" + id).header("Authorization", tokenA).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void preciosMasivosRespetanLaOferta() throws Exception {
        long conOferta = id(crearProducto(tokenA, catA, "Q-1", 10000, 8000L));
        long sinOferta = id(crearProducto(tokenA, catA, "Q-2", 5000, null));

        mvc.perform(patch("/admin/productos/precios").header("Authorization", tokenA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cambios\":[{\"id\":" + sinOferta + ",\"precio\":6000},{\"id\":" + conOferta + ",\"precio\":7000}]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/admin/productos/precios").header("Authorization", tokenA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cambios\":[{\"id\":" + sinOferta + ",\"precio\":6000},{\"id\":" + conOferta + ",\"precio\":12000}]}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/admin/productos/" + sinOferta).header("Authorization", tokenA)).andExpect(jsonPath("$.precio").value(6000));
    }

    @Test
    void laFotoSeGuardaEnJpgYUnaGiganteONoImagenSeRechaza() throws Exception {
        long p = id(crearProducto(tokenA, catA, "Q-1", 10000, null));

        mvc.perform(multipart("/admin/productos/" + p + "/imagen").file(png("archivo", 60, 40)).header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagenUrl").value(startsWith("/api/uploads/")))
                .andExpect(jsonPath("$.imagenThumbUrl").value(startsWith("/api/uploads/")));
        mvc.perform(multipart("/admin/productos/" + p + "/imagen").file(png("archivo", 8001, 1)).header("Authorization", tokenA))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("8000")));
        mvc.perform(multipart("/admin/productos/" + p + "/imagen")
                        .file(new MockMultipartFile("archivo", "x.png", "image/png", "no soy una foto".getBytes()))
                        .header("Authorization", tokenA))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("JPG, PNG o WebP")));
    }

    @Test
    void categoriasConFranjaUnicaYSinBorrarLasQueTienenProductos() throws Exception {
        long combos = id(post("/admin/categorias").header("Authorization", tokenA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Combos\",\"activa\":true}"));
        mvc.perform(post("/admin/categorias").header("Authorization", tokenA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"quesos\",\"activa\":true}")).andExpect(status().isConflict());

        mvc.perform(multipart("/admin/categorias/" + catA + "/franja").file(png("archivo", 320, 120))
                        .param("titulo", "Quesos de la casa").with(r -> { r.setMethod("PUT"); return r; })
                        .header("Authorization", tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.bannerActivo").value(true));
        mvc.perform(multipart("/admin/categorias/" + combos + "/franja").file(png("archivo", 320, 120))
                        .with(r -> { r.setMethod("PUT"); return r; }).header("Authorization", tokenA))
                .andExpect(status().isOk());
        mvc.perform(get("/admin/categorias").header("Authorization", tokenA))
                .andExpect(jsonPath("$[?(@.bannerActivo == true)]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.bannerActivo == true)].nombre").value("Combos"));

        mvc.perform(put("/admin/categorias/orden").header("Authorization", tokenA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[" + combos + "," + catA + "]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].nombre").value("Combos"));

        crearProducto(tokenA, catA, "Q-1", 10000, null);
        mvc.perform(delete("/admin/categorias/" + catA).header("Authorization", tokenA)).andExpect(status().isConflict());
        mvc.perform(delete("/admin/categorias/" + combos).header("Authorization", tokenA)).andExpect(status().isNoContent());
    }

    @Test
    void elInicioDelPanelCuentaProductosSinFotoAgotadosYOfertas() throws Exception {
        crearProducto(tokenA, catA, "Q-1", 10000, 8000L);
        long agotado = id(crearProducto(tokenA, catA, "Q-2", 5000, null));
        mvc.perform(patch("/admin/productos/" + agotado + "/disponible").header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON).content("{\"disponible\":false}")).andExpect(status().isOk());

        mvc.perform(get("/admin/dashboard").header("Authorization", tokenA))
                .andExpect(jsonPath("$.productos").value(2))
                .andExpect(jsonPath("$.sinFoto").value(2))
                .andExpect(jsonPath("$.agotados").value(1))
                .andExpect(jsonPath("$.ofertasVigentes").value(1))
                .andExpect(jsonPath("$.linkPublico").value(startsWith("/c/empresa-a-" + sufijo)));
    }

    private String admin(String letra) throws Exception {
        long empresa = jdbc.queryForObject("INSERT INTO main.empresa (nombre, slug, public_hash) VALUES (?, ?, ?) RETURNING id",
                Long.class, "Empresa " + letra, "empresa-" + letra + "-" + sufijo, letra + sufijo.substring(1));
        String email = letra + "-" + sufijo + "@prueba.local";
        jdbc.update("INSERT INTO main.usuario (empresa_id, nombre, email, password_hash, rol) VALUES (?, 'P', ?, ?, 'ADMIN_EMPRESA')",
                empresa, email, passwordEncoder.encode(CLAVE));
        String sesion = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + CLAVE + "\"}")).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(sesion, "$.accessToken");
    }

    private ResultActions crearProducto(String token, long categoria, String codigo, long precio, Long oferta) throws Exception {
        return mvc.perform(post("/admin/productos").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(producto(categoria, codigo, precio, oferta)));
    }

    private static String producto(long categoria, String codigo, long precio, Long oferta) {
        return """
                {"codigo":"%s","categoriaId":%d,"nombre":"Producto %s","descripcion":null,"presentacion":"Bloque",
                 "unidadVenta":"LIBRA","precio":%d,"precioOferta":%s,"ofertaDesde":null,"ofertaHasta":null,
                 "disponible":true,"destacado":false}
                """.formatted(codigo, categoria, codigo, precio, oferta);
    }

    private long id(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        return id(mvc.perform(request));
    }

    private static long id(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    private static MockMultipartFile png(String nombre, int ancho, int alto) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB), "png", out);
        return new MockMultipartFile(nombre, "foto.png", "image/png", out.toByteArray());
    }
}
