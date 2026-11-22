package com.agora.catalog.modulo;

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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Módulos por empresa: el super admin los enciende y apaga, y cada ruta y el catálogo público los respetan. */
@SpringBootTest(properties = {"settings.security.rate-limit-enabled=false",
        "settings.storage-path=${java.io.tmpdir}/agora-catalog-pruebas"})
@AutoConfigureMockMvc
@Transactional
class ModulosIntegrationTest {

    private static final String CLAVE = "Clave1234";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 8);
    private long empresa;
    private String hash;
    private String admin;
    private String superAdmin;

    @BeforeEach
    void datos() throws Exception {
        hash = "m" + sufijo.substring(1);
        empresa = jdbc.queryForObject("INSERT INTO main.empresa (nombre, slug, public_hash) VALUES (?, ?, ?) RETURNING id",
                Long.class, "Empresa " + sufijo, "empresa-" + sufijo, hash);
        jdbc.update("INSERT INTO main.usuario (empresa_id, nombre, email, password_hash, rol) VALUES (?, 'A', ?, ?, 'ADMIN_EMPRESA')",
                empresa, "a-" + sufijo + "@prueba.local", passwordEncoder.encode(CLAVE));
        jdbc.update("INSERT INTO main.usuario (empresa_id, nombre, email, password_hash, rol) VALUES (NULL, 'S', ?, ?, 'SUPER_ADMIN')",
                "s-" + sufijo + "@prueba.local", passwordEncoder.encode(CLAVE));
        admin = login("a-" + sufijo + "@prueba.local");
        superAdmin = login("s-" + sufijo + "@prueba.local");
    }

    @Test
    void unaEmpresaNuevaArrancaSinModulosYElCatalogoBasicoFunciona() throws Exception {
        mvc.perform(get("/superadmin/empresas/" + empresa + "/modulos").header("Authorization", superAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[?(@.activo == true)]", hasSize(0)))
                .andExpect(jsonPath("$[4].modulo").value("VENTA_RUTA"))
                .andExpect(jsonPath("$[4].nombre").value("Venta en ruta"));
        mvc.perform(get("/admin/modulos").header("Authorization", admin)).andExpect(jsonPath("$", hasSize(0)));

        // Cada módulo cerrado responde 403 con un mensaje para el negocio.
        for (String ruta : new String[]{"/admin/carros", "/admin/tablero-ruta", "/admin/pedidos-tienda/1", "/admin/colecciones",
                "/admin/importacion/plantilla", "/admin/estadisticas"}) {
            mvc.perform(get(ruta).header("Authorization", admin))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(containsString("Tu plan no incluye")));
        }
        // Lo básico no es un módulo: sigue abierto.
        mvc.perform(get("/admin/productos").header("Authorization", admin)).andExpect(status().isOk());
        mvc.perform(get("/admin/categorias").header("Authorization", admin)).andExpect(status().isOk());
        mvc.perform(get("/admin/empresa").header("Authorization", admin)).andExpect(status().isOk());
    }

    @Test
    void encenderAbreElModuloYApagarLoCierraSinBorrarSusDatos() throws Exception {
        modulo("VENTA_RUTA", true).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.modulo == 'VENTA_RUTA')].activo").value(true));
        mvc.perform(get("/admin/modulos").header("Authorization", admin)).andExpect(jsonPath("$[0]").value("VENTA_RUTA"));
        String carro = mvc.perform(post("/admin/carros").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Carro 1\",\"placa\":null,\"activo\":true}")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long carroId = ((Number) JsonPath.read(carro, "$.id")).longValue();

        modulo("VENTA_RUTA", false).andExpect(status().isOk());
        mvc.perform(get("/admin/carros/" + carroId).header("Authorization", admin))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("Venta en ruta")));

        modulo("VENTA_RUTA", true);
        mvc.perform(get("/admin/carros/" + carroId).header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Carro 1"));
    }

    @Test
    void elCatalogoPublicoRespetaLosModulos() throws Exception {
        long cat = jdbc.queryForObject("INSERT INTO main.categoria (empresa_id, nombre, activa, orden) VALUES (?, 'Quesos', TRUE, 1) RETURNING id",
                Long.class, empresa);
        long producto = jdbc.queryForObject("""
                INSERT INTO main.producto (empresa_id, categoria_id, codigo, nombre, unidad_venta, precio)
                VALUES (?, ?, 'Q-1', 'Queso', 'LIBRA', 18000) RETURNING id
                """, Long.class, empresa, cat);
        long coleccion = jdbc.queryForObject("INSERT INTO main.coleccion (empresa_id, nombre) VALUES (?, 'Picada') RETURNING id",
                Long.class, empresa);
        jdbc.update("INSERT INTO main.coleccion_producto (coleccion_id, producto_id) VALUES (?, ?)", coleccion, producto);

        // Configuración dice "recibir pedidos" (por defecto), pero sin los módulos no hay carrito ni colecciones.
        mvc.perform(get("/public/catalogos/" + hash))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.negocio.pedidosWhatsapp").value(false))
                .andExpect(jsonPath("$.colecciones", hasSize(0)))
                .andExpect(jsonPath("$.productos", hasSize(1)));

        modulo("PEDIDO_WHATSAPP", true);
        modulo("COLECCIONES", true);
        mvc.perform(get("/public/catalogos/" + hash))
                .andExpect(jsonPath("$.negocio.pedidosWhatsapp").value(true))
                .andExpect(jsonPath("$.colecciones", hasSize(1)));
    }

    @Test
    void soloElSuperAdminCambiaLosModulos() throws Exception {
        mvc.perform(get("/superadmin/empresas/" + empresa + "/modulos").header("Authorization", admin)).andExpect(status().isForbidden());
        mvc.perform(patch("/superadmin/empresas/" + empresa + "/modulos/VENTA_RUTA").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":true}")).andExpect(status().isForbidden());
        mvc.perform(get("/admin/modulos").header("Authorization", superAdmin)).andExpect(status().isForbidden());
        mvc.perform(get("/superadmin/empresas/999999999/modulos").header("Authorization", superAdmin)).andExpect(status().isNotFound());
        modulo("NO_EXISTE", true).andExpect(status().isBadRequest());
    }

    private ResultActions modulo(String modulo, boolean activo) throws Exception {
        return mvc.perform(patch("/superadmin/empresas/" + empresa + "/modulos/" + modulo).header("Authorization", superAdmin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":" + activo + "}"));
    }

    private String login(String email) throws Exception {
        String sesion = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + CLAVE + "\"}")).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(sesion, "$.accessToken");
    }
}
