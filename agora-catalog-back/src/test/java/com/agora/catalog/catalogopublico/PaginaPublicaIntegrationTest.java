package com.agora.catalog.catalogopublico;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** /c/{slug}-{hash}: etiquetas Open Graph escapadas, 301 por slug viejo y empresa inactiva. */
@SpringBootTest(properties = {"settings.security.rate-limit-enabled=false",
        "settings.frontend-url=https://catalogo.test/",
        "settings.storage-path=${java.io.tmpdir}/agora-catalog-pruebas",
        "settings.front-index-path=${java.io.tmpdir}/agora-catalog-pruebas/index.html"})
@AutoConfigureMockMvc
@Transactional
class PaginaPublicaIntegrationTest {

    private static final Path CARPETA = Path.of(System.getProperty("java.io.tmpdir"), "agora-catalog-pruebas");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    private final String hash = "g" + UUID.randomUUID().toString().substring(1, 8);
    private final String link = "quesera-" + hash;
    private long empresaId;

    @BeforeEach
    void datos() throws Exception {
        Files.createDirectories(CARPETA);
        Files.writeString(CARPETA.resolve("index.html"),
                "<html><head><title>Agorá Catalog</title><!--og--></head><body><app-root></app-root></body></html>");
        empresaId = jdbc.queryForObject("""
                INSERT INTO main.empresa (nombre, slug, public_hash, portada_subtitulo)
                VALUES (?, ?, ?, 'Quesos "frescos" & <b>más</b>') RETURNING id
                """, Long.class, "Quesera <script>", "quesera", hash);
    }

    @Test
    void escribeLasEtiquetasEscapadasYUsaUnaPortadaLiviana() throws Exception {
        Path portada = CARPETA.resolve(empresaId + "/portada/" + hash + ".jpg");
        Files.createDirectories(portada.getParent());
        ImageIO.write(new BufferedImage(3200, 1508, BufferedImage.TYPE_INT_RGB), "jpg", portada.toFile());
        jdbc.update("UPDATE main.empresa SET portada_url = ? WHERE id = ?",
                "/api/uploads/" + empresaId + "/portada/" + hash + ".jpg", empresaId);

        mvc.perform(get("/public/paginas/" + link))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>Quesera &lt;script&gt;</title>")))
                .andExpect(content().string(containsString(
                        "<meta property=\"og:description\" content=\"Quesos &quot;frescos&quot; &amp; &lt;b&gt;más&lt;/b&gt;\">")))
                .andExpect(content().string(containsString(
                        "<meta property=\"og:url\" content=\"https://catalogo.test/c/" + link + "\">")))
                .andExpect(content().string(containsString("https://catalogo.test/api/uploads/" + empresaId + "/portada/" + hash + "-og.jpg")))
                .andExpect(content().string(not(containsString("<!--og-->"))));

        BufferedImage og = ImageIO.read(CARPETA.resolve(empresaId + "/portada/" + hash + "-og.jpg").toFile());
        assertThat(og.getWidth()).isEqualTo(1200);
    }

    @Test
    void unSlugViejoRedirigeAlActualConservandoLaSubpagina() throws Exception {
        mvc.perform(get("/public/paginas/nombre-viejo-" + hash + "/catalogo?q=queso"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/c/" + link + "/catalogo?q=queso"));
        mvc.perform(get("/public/paginas/" + hash))
                .andExpect(status().isMovedPermanently());
        // Sin portada ni logo: la imagen de Agorá Catalog.
        mvc.perform(get("/public/paginas/" + link))
                .andExpect(content().string(containsString("content=\"https://catalogo.test/og-default.jpg\"")));
    }

    @Test
    void unProductoCompartidoTieneSuPropiaVistaPrevia() throws Exception {
        long cat = jdbc.queryForObject("INSERT INTO main.categoria (empresa_id, nombre) VALUES (?, 'Quesos') RETURNING id",
                Long.class, empresaId);
        long id = jdbc.queryForObject("""
                INSERT INTO main.producto (empresa_id, categoria_id, codigo, nombre, presentacion, unidad_venta, precio,
                                           precio_oferta, imagen_url)
                VALUES (?, ?, 'Q-1', 'Queso campesino', 'Bloque x 500 g', 'UNIDAD', 16800, 14900, '/api/uploads/x/q.jpg')
                RETURNING id
                """, Long.class, empresaId, cat);

        mvc.perform(get("/public/paginas/" + link + "/catalogo").param("p", String.valueOf(id)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>Queso campesino · Quesera &lt;script&gt;</title>")))
                .andExpect(content().string(containsString("content=\"$14.900 (antes $16.800) · Bloque x 500 g\"")))
                .andExpect(content().string(containsString("content=\"https://catalogo.test/api/uploads/x/q.jpg\"")))
                .andExpect(content().string(containsString("content=\"https://catalogo.test/c/" + link + "/catalogo?p=" + id + "\"")));

        // Con los precios ocultos, la vista previa tampoco los muestra.
        jdbc.update("UPDATE main.empresa SET mostrar_precios = FALSE WHERE id = ?", empresaId);
        mvc.perform(get("/public/paginas/" + link + "/catalogo").param("p", String.valueOf(id)))
                .andExpect(content().string(not(containsString("$14.900"))))
                .andExpect(content().string(containsString("content=\"Bloque x 500 g\"")));

        // Un producto que no es de este negocio: la vista previa del negocio.
        mvc.perform(get("/public/paginas/" + link + "/catalogo").param("p", "999999999"))
                .andExpect(content().string(containsString("<title>Quesera &lt;script&gt;</title>")));
    }

    @Test
    void unaEmpresaInactivaOInexistenteDaLaPaginaSinEtiquetasY404() throws Exception {
        jdbc.update("UPDATE main.empresa SET activa = FALSE WHERE id = ?", empresaId);
        mvc.perform(get("/public/paginas/" + link))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("og:title"))));
        mvc.perform(get("/public/paginas/no-existe-zzzzzzzz")).andExpect(status().isNotFound());
    }
}
