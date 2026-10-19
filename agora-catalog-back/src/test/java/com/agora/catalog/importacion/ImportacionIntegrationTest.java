package com.agora.catalog.importacion;

import com.jayway.jsonpath.JsonPath;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Productos desde Excel y fotos por ZIP, siempre dentro de la empresa del token. */
@SpringBootTest(properties = {"settings.security.rate-limit-enabled=false",
        "settings.storage-path=${java.io.tmpdir}/agora-catalog-pruebas"})
@AutoConfigureMockMvc
@Transactional
class ImportacionIntegrationTest {

    private static final String CLAVE = "Clave1234";
    private static final String[] TITULOS = {"codigo*", "categoria*", "nombre*", "descripcion", "presentacion",
            "unidad_venta*", "precio*", "precio_oferta", "oferta_desde", "oferta_hasta", "disponible", "destacado"};

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 8);
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void datos() throws Exception {
        tokenA = admin("a");
        tokenB = admin("b");
        mvc.perform(post("/admin/categorias").header("Authorization", tokenA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Quesos\",\"activa\":true}")).andExpect(status().isCreated());
    }

    @Test
    void laPlantillaTraeLosTitulosYLasInstrucciones() throws Exception {
        byte[] xlsx = mvc.perform(get("/admin/importacion/plantilla").header("Authorization", tokenA))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertThat(wb.getSheet("Productos").getRow(0).getCell(0).getStringCellValue()).isEqualTo("codigo*");
            assertThat(wb.getSheet("Instrucciones")).isNotNull();
        }
    }

    @Test
    void conUnErrorNoSeGuardaNada() throws Exception {
        MockMultipartFile excel = excel(
                new Object[]{"Q-1", "Quesos", "Queso campesino", null, "Bloque", "libra", 12000, null, null, null, "SI", null},
                new Object[]{"Q-2", "Quesos", "Queso doble crema", null, null, "UNIDAD", "$9.500", 9500, null, null, null, null},
                new Object[]{"q-1", "Quesos", "Repetido", null, null, "METRO", 1, null, null, null, "TAL VEZ", null});

        mvc.perform(multipart("/admin/importacion/previsualizar").file(excel).header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filas").value(3))
                .andExpect(jsonPath("$.errores", hasSize(2)))
                .andExpect(jsonPath("$.errores[0].fila").value(3))
                .andExpect(jsonPath("$.errores[0].mensaje").value(containsString("menor que el precio")))
                .andExpect(jsonPath("$.errores[1].fila").value(4))
                .andExpect(jsonPath("$.errores[1].mensaje").value(containsString("ya está en la fila 2")))
                .andExpect(jsonPath("$.errores[1].mensaje").value(containsString("SI o NO")))
                .andExpect(jsonPath("$.errores[1].mensaje").value(containsString("unidad de venta")));
        mvc.perform(multipart("/admin/importacion/confirmar").file(excel).header("Authorization", tokenA))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.aplicado").value(false));
        mvc.perform(get("/admin/productos").header("Authorization", tokenA)).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void creaActualizaYCreaLasCategoriasQueFaltan() throws Exception {
        MockMultipartFile primera = excel(
                new Object[]{"Q-1", "quesos", "Queso campesino", null, "Bloque", "LIBRA", 12000, 10000, "01/12/2026", "31/12/2026", null, "SI"},
                new Object[]{"C-1", "Carnes frías", "Jamón", null, null, "PAQUETE", "8500", null, null, null, "NO", null});
        mvc.perform(multipart("/admin/importacion/previsualizar").file(primera).header("Authorization", tokenA))
                .andExpect(jsonPath("$.nuevos").value(2))
                .andExpect(jsonPath("$.categoriasNuevas[0]").value("Carnes frías"))
                .andExpect(jsonPath("$.aplicado").value(false));
        mvc.perform(multipart("/admin/importacion/confirmar").file(primera).header("Authorization", tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.aplicado").value(true));

        mvc.perform(multipart("/admin/importacion/confirmar").header("Authorization", tokenA).file(excel(
                        new Object[]{"q-1", "Quesos", "Queso campesino x libra", null, null, "LIBRA", 13000, null, null, null, null, null})))
                .andExpect(jsonPath("$.nuevos").value(0)).andExpect(jsonPath("$.actualizados").value(1));

        mvc.perform(get("/admin/productos").param("q", "Q-1").header("Authorization", tokenA))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].precio").value(13000))
                .andExpect(jsonPath("$.content[0].precioOferta").doesNotExist());
        mvc.perform(get("/admin/productos").param("q", "C-1").header("Authorization", tokenA))
                .andExpect(jsonPath("$.content[0].disponible").value(false))
                .andExpect(jsonPath("$.content[0].categoriaNombre").value("Carnes frías"));

        // La otra empresa no ve nada de esto, y su exportación sale vacía.
        mvc.perform(get("/admin/productos").header("Authorization", tokenB)).andExpect(jsonPath("$.totalElements").value(0));
        byte[] exportB = mvc.perform(get("/admin/importacion/exportar").header("Authorization", tokenB))
                .andReturn().getResponse().getContentAsByteArray();
        byte[] exportA = mvc.perform(get("/admin/importacion/exportar").header("Authorization", tokenA))
                .andReturn().getResponse().getContentAsByteArray();
        try (Workbook b = new XSSFWorkbook(new ByteArrayInputStream(exportB));
             Workbook a = new XSSFWorkbook(new ByteArrayInputStream(exportA))) {
            assertThat(b.getSheet("Productos").getLastRowNum()).isZero();
            assertThat(a.getSheet("Productos").getLastRowNum()).isEqualTo(2);
        }
    }

    @Test
    void elZipAsignaFotosPorCodigoSoloEnLaEmpresaPropia() throws Exception {
        mvc.perform(multipart("/admin/importacion/confirmar").header("Authorization", tokenA).file(excel(
                        new Object[]{"Q-1", "Quesos", "Queso", null, null, "LIBRA", 12000, null, null, null, null, null})))
                .andExpect(status().isOk());

        MockMultipartFile zip = zip("q-1.png", png(), "fotos/SIN-PRODUCTO.jpg", png(), "__MACOSX/._q-1.png", new byte[]{1},
                "notas.txt", "hola".getBytes());
        mvc.perform(multipart("/admin/importacion/imagenes").file(zip).header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asignadas[0]").value("q-1.png"))
                .andExpect(jsonPath("$.sinProducto[0]").value("SIN-PRODUCTO.jpg"))
                .andExpect(jsonPath("$.conError", hasSize(1)));
        mvc.perform(get("/admin/productos").param("q", "Q-1").header("Authorization", tokenA))
                .andExpect(jsonPath("$.content[0].imagenUrl").exists());

        // El mismo ZIP en la otra empresa no encuentra el producto.
        mvc.perform(multipart("/admin/importacion/imagenes").file(zip).header("Authorization", tokenB))
                .andExpect(jsonPath("$.asignadas", hasSize(0)))
                .andExpect(jsonPath("$.sinProducto", hasSize(2)));
    }

    @Test
    void unArchivoQueNoEsExcelOZipSeRechaza() throws Exception {
        MockMultipartFile basura = new MockMultipartFile("archivo", "x.xlsx", "application/octet-stream", "basura".getBytes());
        mvc.perform(multipart("/admin/importacion/previsualizar").file(basura).header("Authorization", tokenA))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString(".xlsx")));
        mvc.perform(multipart("/admin/importacion/imagenes").file(basura).header("Authorization", tokenA))
                .andExpect(status().isBadRequest());
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

    /** Excel con la hoja Productos; un número va como celda numérica y un texto como texto. */
    private static MockMultipartFile excel(Object[]... filas) throws Exception {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = wb.createSheet("Productos");
            Row cabecera = hoja.createRow(0);
            for (int i = 0; i < TITULOS.length; i++) cabecera.createCell(i).setCellValue(TITULOS[i]);
            for (int r = 0; r < filas.length; r++) {
                Row row = hoja.createRow(r + 1);
                for (int c = 0; c < filas[r].length; c++) {
                    Object v = filas[r][c];
                    if (v instanceof Number n) row.createCell(c).setCellValue(n.doubleValue());
                    else if (v != null) row.createCell(c).setCellValue(v.toString());
                }
            }
            wb.write(out);
            return new MockMultipartFile("archivo", "productos.xlsx", "application/octet-stream", out.toByteArray());
        }
    }

    private static MockMultipartFile zip(Object... nombresYContenidos) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(out)) {
            for (int i = 0; i < nombresYContenidos.length; i += 2) {
                z.putNextEntry(new ZipEntry((String) nombresYContenidos[i]));
                z.write((byte[]) nombresYContenidos[i + 1]);
                z.closeEntry();
            }
        }
        return new MockMultipartFile("archivo", "fotos.zip", "application/zip", out.toByteArray());
    }

    private static byte[] png() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(60, 40, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }
}
