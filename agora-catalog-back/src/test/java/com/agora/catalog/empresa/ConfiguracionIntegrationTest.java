package com.agora.catalog.empresa;

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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Configuración del negocio (textos, colores, WhatsApp, sellos, logo y portada) y colecciones. */
@SpringBootTest(properties = {"settings.security.rate-limit-enabled=false",
        "settings.storage-path=${java.io.tmpdir}/agora-catalog-pruebas"})
@AutoConfigureMockMvc
@Transactional
class ConfiguracionIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 8);
    private String token;
    private String hash;

    @BeforeEach
    void datos() throws Exception {
        hash = "c" + sufijo.substring(1);
        long empresa = jdbc.queryForObject("INSERT INTO main.empresa (nombre, slug, public_hash) VALUES (?, ?, ?) RETURNING id",
                Long.class, "Tienda " + sufijo, "tienda-" + sufijo, hash);
        String email = "c-" + sufijo + "@prueba.local";
        jdbc.update("INSERT INTO main.usuario (empresa_id, nombre, email, password_hash, rol) VALUES (?, 'P', ?, ?, 'ADMIN_EMPRESA')",
                empresa, email, passwordEncoder.encode("Clave1234"));
        String sesion = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Clave1234\"}")).andReturn().getResponse().getContentAsString();
        token = "Bearer " + JsonPath.read(sesion, "$.accessToken");
    }

    @Test
    void laConfiguracionSeGuardaYLaVeElCatalogoPublico() throws Exception {
        guardar(config("#24331F", "#DDB06C", "[{\"numero\":\"573163218276\",\"nombre\":\"Pedidos\"}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portadaTituloAcento").value("Sabe a\nun buen plan."))
                .andExpect(jsonPath("$.sellos", hasSize(1)));
        mvc.perform(get("/public/catalogos/" + hash))
                .andExpect(jsonPath("$.negocio.nombreCorto").value("Tienda"))
                .andExpect(jsonPath("$.negocio.fachada").value("PANADERIA"))
                .andExpect(jsonPath("$.whatsapps[0].nombre").value("Pedidos"))
                .andExpect(jsonPath("$.sellos[0].icono").value("DOMICILIO"))
                .andExpect(jsonPath("$.horario", hasSize(2)))
                .andExpect(jsonPath("$.horario[1].dia").value(7))
                .andExpect(jsonPath("$.horario[1].cierra").value("13:00"))
                .andExpect(jsonPath("$.zonaHoraria").value("America/Bogota"));
    }

    @Test
    void elHorarioNoAceptaDiasRepetidosNiCierreAntesDeAbrir() throws Exception {
        String base = config("#24331F", "#DDB06C", "[]");
        guardar(base.replace("{\"dia\":7,\"abre\":\"08:00\"", "{\"dia\":1,\"abre\":\"08:00\""))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("repetido")));
        guardar(base.replace("\"cierra\":\"13:00\"", "\"cierra\":\"08:00\""))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("cerrar")));
        guardar(base.replace("\"cierra\":\"13:00\"", "\"cierra\":\"25:00\"")).andExpect(status().isBadRequest());
        guardar(base.replace("\"horarioSemana\":[{\"dia\":1,\"abre\":\"07:00\",\"cierra\":\"19:00\"},", "\"horarioSemana\":["))
                .andExpect(status().isOk()).andExpect(jsonPath("$.horarioSemana", hasSize(1)));
    }

    @Test
    void noSeGuardanColoresSinContrasteNiWhatsappRepetidosOEnExceso() throws Exception {
        guardar(config("#F2E9D0", "#DDB06C", "[]")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("color de botones")));
        guardar(config("#24331F", "#2A2A2A", "[]")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("acento")));
        guardar(config("#24331F", "#DDB06C", "[{\"numero\":\"573001112233\"},{\"numero\":\"573001112233\"}]"))
                .andExpect(status().isBadRequest());
        guardar(config("#24331F", "#DDB06C",
                "[{\"numero\":\"573001112231\"},{\"numero\":\"573001112232\"},{\"numero\":\"573001112233\"},{\"numero\":\"573001112234\"}]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elLogoPuedeAlejarseYLaPortadaNoSeSaleDeLaFoto() throws Exception {
        foto("logo", png(400, 300), -50, -100, 500, 500).andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value(startsWith("/api/uploads/")))
                .andExpect(jsonPath("$.logoRecorte.x").value(-50));
        foto("portada", png(1600, 800), -10, 0, 1600, 754).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("se sale")));

        String primera = JsonPath.read(foto("portada", png(1600, 800), 0, 0, 1600, 754)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.portadaUrl");
        // Reencuadrar sin volver a subir la foto: usa el original guardado.
        foto("portada", null, 0, 40, 1600, 754).andExpect(status().isOk())
                .andExpect(jsonPath("$.portadaUrl").value(not(primera)))
                .andExpect(jsonPath("$.portadaRecorte.y").value(40));
    }

    @Test
    void coleccionesSoloConProductosPropiosYHastaSeis() throws Exception {
        long ajeno = jdbc.queryForObject("SELECT min(id) FROM main.producto", Long.class) == null ? 999_999
                : jdbc.queryForObject("SELECT min(id) FROM main.producto", Long.class);
        coleccion("Picada", "[" + ajeno + "]").andExpect(status().isNotFound());
        for (int i = 1; i <= 6; i++) coleccion("Plan " + i, "[]").andExpect(status().isCreated());
        coleccion("Plan 7", "[]").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("hasta 6")));

        mvc.perform(put("/admin/colecciones/seccion").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"¿Qué se te antoja?\",\"texto\":null,\"nota\":\"Para hoy\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.coleccionesTitulo").value("¿Qué se te antoja?"));
    }

    private ResultActions guardar(String json) throws Exception {
        return mvc.perform(put("/admin/empresa").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String config(String primario, String acento, String whatsapps) {
        return """
                {"nombre":"Tienda %1$s","slug":"tienda-%1$s","nombreCorto":"Tienda","lema":"Del barrio",
                 "portadaAntetitulo":null,"portadaTitulo":"Se ve rico.","portadaTituloAcento":"Sabe a\\nun buen plan.",
                 "portadaSubtitulo":null,"portadaNota":null,"portadaSelloTexto":null,"catalogoTitulo":null,"catalogoAcento":null,
                 "catalogoSubtitulo":null,"visitaNota":null,"fachada":"PANADERIA","adornos":true,"direccion":"Calle 1",
                 "horario":null,"infoDomicilios":null,"mostrarPrecios":true,"pedidosWhatsapp":true,"mostrarAgotados":true,
                 "colorPrimario":"%2$s","colorSecundario":"%3$s","whatsapps":%4$s,
                 "sellos":[{"icono":"DOMICILIO","texto":"Domicilios en el barrio"}],
                 "horarioSemana":[{"dia":1,"abre":"07:00","cierra":"19:00"},{"dia":7,"abre":"08:00","cierra":"13:00"}]}
                """.formatted(sufijo, primario, acento, whatsapps);
    }

    private ResultActions foto(String tipo, MockMultipartFile archivo, int x, int y, int ancho, int alto) throws Exception {
        var req = multipart("/admin/empresa/" + tipo);
        if (archivo != null) req.file(archivo);
        return mvc.perform(req.param("x", "" + x).param("y", "" + y).param("ancho", "" + ancho).param("alto", "" + alto)
                .header("Authorization", token));
    }

    private ResultActions coleccion(String nombre, String productos) throws Exception {
        return mvc.perform(post("/admin/colecciones").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"" + nombre + "\",\"descripcion\":null,\"visible\":true,\"productos\":" + productos + "}"));
    }

    private static MockMultipartFile png(int ancho, int alto) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB), "png", out);
        return new MockMultipartFile("archivo", "foto.png", "image/png", out.toByteArray());
    }
}
