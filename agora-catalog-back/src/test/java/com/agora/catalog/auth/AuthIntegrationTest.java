package com.agora.catalog.auth;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Login, roles, aislamiento entre empresas y desactivación, contra la base del {@code .env}.
 * Cada prueba corre en una transacción que se revierte: no deja datos.
 */
@SpringBootTest(properties = "settings.security.rate-limit-enabled=false")
@AutoConfigureMockMvc
@Transactional
class AuthIntegrationTest {

    private static final String CLAVE = "Clave1234";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 8);
    private long empresaA;
    private long empresaB;
    private long adminA;
    private String emailAdminA;
    private String emailAdminB;
    private String emailSuper;

    @BeforeEach
    void datos() {
        empresaA = empresa("Empresa A " + sufijo, "empresa-a-" + sufijo, "a" + sufijo.substring(1));
        empresaB = empresa("Empresa B " + sufijo, "empresa-b-" + sufijo, "b" + sufijo.substring(1));
        emailAdminA = "a-" + sufijo + "@prueba.local";
        emailAdminB = "b-" + sufijo + "@prueba.local";
        emailSuper = "super-" + sufijo + "@prueba.local";
        adminA = usuario(empresaA, emailAdminA, "ADMIN_EMPRESA");
        usuario(empresaB, emailAdminB, "ADMIN_EMPRESA");
        usuario(null, emailSuper, "SUPER_ADMIN");
    }

    @Test
    void elLoginEntregaTokensYCadaAdminVeSoloSuEmpresa() throws Exception {
        login(emailAdminA, CLAVE).andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.rol").value("ADMIN_EMPRESA"))
                .andExpect(jsonPath("$.usuario.empresaId").value(empresaA));

        mvc.perform(get("/admin/empresa").header("Authorization", bearer(emailAdminA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Empresa A " + sufijo));
        mvc.perform(get("/admin/empresa").header("Authorization", bearer(emailAdminB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Empresa B " + sufijo));
    }

    @Test
    void contrasenaIncorrectaYCorreoInexistenteDanElMismoMensaje() throws Exception {
        String incorrecta = body(login(emailAdminA, "otra-clave").andExpect(status().isUnauthorized()));
        String inexistente = body(login("nadie-" + sufijo + "@prueba.local", CLAVE).andExpect(status().isUnauthorized()));
        assertThat((String) JsonPath.read(incorrecta, "$.message")).isEqualTo(JsonPath.read(inexistente, "$.message"));
    }

    @Test
    void cadaRolSoloEntraASusRutas() throws Exception {
        mvc.perform(get("/admin/empresa")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/empresa").header("Authorization", "Bearer no-es-un-jwt"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/superadmin/empresas").header("Authorization", bearer(emailAdminA)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/empresa").header("Authorization", bearer(emailSuper)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/superadmin/empresas").header("Authorization", bearer(emailSuper)))
                .andExpect(status().isOk());
    }

    @Test
    void elRefreshRotaYUnTokenUsadoNoSirveDosVeces() throws Exception {
        String refresh = JsonPath.read(body(login(emailAdminA, CLAVE)), "$.refreshToken");

        String nuevo = JsonPath.read(body(refresh(refresh).andExpect(status().isOk())), "$.refreshToken");
        assertThat(nuevo).isNotEqualTo(refresh);
        refresh(refresh).andExpect(status().isUnauthorized());

        mvc.perform(post("/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + nuevo + "\"}"))
                .andExpect(status().isNoContent());
        refresh(nuevo).andExpect(status().isUnauthorized());
    }

    @Test
    void desactivarUnUsuarioCortaSuLoginYSuRefresh() throws Exception {
        String refresh = JsonPath.read(body(login(emailAdminA, CLAVE)), "$.refreshToken");

        mvc.perform(patch("/superadmin/usuarios/" + adminA + "/activo").header("Authorization", bearer(emailSuper))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isNoContent());

        refresh(refresh).andExpect(status().isUnauthorized());
        login(emailAdminA, CLAVE).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("desactivada")));
    }

    @Test
    void desactivarUnaEmpresaCortaElLoginYElRefreshDeSusUsuarios() throws Exception {
        String refresh = JsonPath.read(body(login(emailAdminA, CLAVE)), "$.refreshToken");

        mvc.perform(patch("/superadmin/empresas/" + empresaA + "/activa").header("Authorization", bearer(emailSuper))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isNoContent());

        refresh(refresh).andExpect(status().isUnauthorized());
        login(emailAdminA, CLAVE).andExpect(status().isUnauthorized());
        login(emailAdminB, CLAVE).andExpect(status().isOk());
    }

    @Test
    void elSuperAdminNoSeDesactivaASiMismo() throws Exception {
        long id = jdbc.queryForObject("SELECT id FROM main.usuario WHERE email = ?", Long.class, emailSuper);
        mvc.perform(patch("/superadmin/usuarios/" + id + "/activo").header("Authorization", bearer(emailSuper))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void restablecerLaContrasenaCierraSusSesionesYPideCambiarla() throws Exception {
        String refresh = JsonPath.read(body(login(emailAdminA, CLAVE)), "$.refreshToken");

        mvc.perform(post("/superadmin/usuarios/" + adminA + "/password").header("Authorization", bearer(emailAdminB)))
                .andExpect(status().isForbidden());
        String r = body(mvc.perform(post("/superadmin/usuarios/" + adminA + "/password").header("Authorization", bearer(emailSuper)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.correoEnviado").value(false)));
        String temporal = JsonPath.read(r, "$.passwordTemporal");

        refresh(refresh).andExpect(status().isUnauthorized());
        login(emailAdminA, CLAVE).andExpect(status().isUnauthorized());
        login(emailAdminA, temporal).andExpect(jsonPath("$.usuario.debeCambiarPassword").value(true));

        long idSuper = jdbc.queryForObject("SELECT id FROM main.usuario WHERE email = ?", Long.class, emailSuper);
        mvc.perform(post("/superadmin/usuarios/" + idSuper + "/password").header("Authorization", bearer(emailSuper)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/superadmin/usuarios/999999999/password").header("Authorization", bearer(emailSuper)))
                .andExpect(status().isNotFound());
    }

    @Test
    void empresaNuevaConAdminQueCambiaLaContrasenaTemporal() throws Exception {
        String creada = body(mvc.perform(post("/superadmin/empresas").header("Authorization", bearer(emailSuper))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Panadería La Espiga " + sufijo + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("panaderia-la-espiga-" + sufijo))
                .andExpect(jsonPath("$.publicHash").value(org.hamcrest.Matchers.matchesPattern("[a-z0-9]{8}"))));
        long id = ((Number) JsonPath.read(creada, "$.id")).longValue();

        mvc.perform(post("/superadmin/empresas").header("Authorization", bearer(emailSuper))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Otra\",\"slug\":\"panaderia-la-espiga-" + sufijo + "\"}"))
                .andExpect(status().isConflict());

        String email = "espiga-" + sufijo + "@prueba.local";
        String admin = body(mvc.perform(post("/superadmin/empresas/" + id + "/usuarios")
                        .header("Authorization", bearer(emailSuper)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Dueña\",\"email\":\"" + email + "\"}"))
                .andExpect(status().isCreated()));
        String temporal = JsonPath.read(admin, "$.passwordTemporal");

        String sesion = body(login(email, temporal).andExpect(jsonPath("$.usuario.debeCambiarPassword").value(true)));
        String token = "Bearer " + JsonPath.read(sesion, "$.accessToken");

        mvc.perform(put("/auth/password").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actual\":\"no-es\",\"nueva\":\"NuevaClave1\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/auth/password").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actual\":\"" + temporal + "\",\"nueva\":\"NuevaClave1\"}"))
                .andExpect(status().isNoContent());
        login(email, "NuevaClave1").andExpect(jsonPath("$.usuario.debeCambiarPassword").value(false));
    }

    private long empresa(String nombre, String slug, String hash) {
        return jdbc.queryForObject("INSERT INTO main.empresa (nombre, slug, public_hash) VALUES (?, ?, ?) RETURNING id",
                Long.class, nombre, slug, hash);
    }

    private long usuario(Long empresaId, String email, String rol) {
        return jdbc.queryForObject("""
                INSERT INTO main.usuario (empresa_id, nombre, email, password_hash, rol)
                VALUES (?, 'Prueba', ?, ?, ?) RETURNING id
                """, Long.class, empresaId, email, passwordEncoder.encode(CLAVE), rol);
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"));
    }

    private String bearer(String email) throws Exception {
        return "Bearer " + JsonPath.read(body(login(email, CLAVE).andExpect(status().isOk())), "$.accessToken");
    }

    private static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }
}
