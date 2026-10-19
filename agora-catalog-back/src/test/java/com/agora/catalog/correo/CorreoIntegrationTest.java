package com.agora.catalog.correo;

import com.agora.catalog.common.encryption.AesEncryption;
import com.agora.catalog.correo.service.SmtpSender;
import com.jayway.jsonpath.JsonPath;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Proveedor de correo y envío de credenciales, con el SMTP reemplazado por un mock. */
@SpringBootTest(properties = "settings.security.rate-limit-enabled=false")
@AutoConfigureMockMvc
@Transactional
class CorreoIntegrationTest {

    private static final String CLAVE = "Clave1234";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AesEncryption aes;
    @MockitoBean
    private SmtpSender smtpSender;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 8);
    private String superToken;
    private long empresaId;

    @BeforeEach
    void datos() throws Exception {
        String email = "super-" + sufijo + "@prueba.local";
        jdbc.update("INSERT INTO main.usuario (nombre, email, password_hash, rol) VALUES ('S', ?, ?, 'SUPER_ADMIN')",
                email, passwordEncoder.encode(CLAVE));
        String sesion = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + CLAVE + "\"}"))
                .andReturn().getResponse().getContentAsString();
        superToken = "Bearer " + JsonPath.read(sesion, "$.accessToken");
        empresaId = jdbc.queryForObject(
                "INSERT INTO main.empresa (nombre, slug, public_hash) VALUES (?, ?, ?) RETURNING id",
                Long.class, "Tienda " + sufijo, "tienda-" + sufijo, "t" + sufijo.substring(1));
    }

    @Test
    void laContrasenaSeGuardaCifradaNoSeDevuelveYSeConservaSiNoSeCambia() throws Exception {
        guardarProveedor("\"secreta-smtp\"", true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordGuardada").value(true))
                .andExpect(jsonPath("$.password").doesNotExist());

        String cifrada = jdbc.queryForObject("SELECT password_cifrada FROM main.proveedor_correo", String.class);
        assertThat(cifrada).isNotEqualTo("secreta-smtp");
        assertThat(aes.decrypt(cifrada)).isEqualTo("secreta-smtp");

        guardarProveedor("\"\"", true).andExpect(jsonPath("$.passwordGuardada").value(true));
        assertThat(aes.decrypt(jdbc.queryForObject("SELECT password_cifrada FROM main.proveedor_correo", String.class)))
                .isEqualTo("secreta-smtp");
    }

    @Test
    void alCrearElAdministradorSeLeEnvianSusDatosYQuedaEnLaBitacora() throws Exception {
        guardarProveedor("\"secreta-smtp\"", true);
        String email = "dueno-" + sufijo + "@prueba.local";

        String admin = crearAdmin(email, true)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correoEnviado").value(true))
                .andReturn().getResponse().getContentAsString();

        String temporal = JsonPath.read(admin, "$.passwordTemporal");
        verify(smtpSender).send(any(), eq("secreta-smtp"), eq(email), contains("Tienda " + sufijo), contains(temporal));
        assertThat(jdbc.queryForObject("SELECT enviado FROM main.correo_envio WHERE destinatario = ?", Boolean.class, email))
                .isTrue();
    }

    @Test
    void siElCorreoFallaElAdministradorIgualQuedaCreado() throws Exception {
        guardarProveedor("\"secreta-smtp\"", true);
        doThrow(new MessagingException("535 Username and Password not accepted"))
                .when(smtpSender).send(any(), any(), any(), any(), any());
        String email = "falla-" + sufijo + "@prueba.local";

        crearAdmin(email, true)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correoEnviado").value(false))
                .andExpect(jsonPath("$.correoError").value(containsString("535")))
                .andExpect(jsonPath("$.passwordTemporal").isNotEmpty());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM main.usuario WHERE email = ?", Integer.class, email))
                .isEqualTo(1);
    }

    @Test
    void sinProveedorActivoNoSeIntentaEnviar() throws Exception {
        guardarProveedor("\"secreta-smtp\"", false);

        crearAdmin("inactivo-" + sufijo + "@prueba.local", true)
                .andExpect(jsonPath("$.correoEnviado").value(false))
                .andExpect(jsonPath("$.correoError").value(containsString("proveedor de correo activo")));
        verify(smtpSender, never()).send(any(), any(), any(), any(), any());
    }

    private ResultActions guardarProveedor(String password, boolean activo) throws Exception {
        return mvc.perform(put("/superadmin/correo").header("Authorization", superToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"host":"smtp.prueba.local","puerto":587,"usuario":"envios@prueba.local","password":%s,
                         "remitenteEmail":"envios@prueba.local","remitenteNombre":"Agorá Catalog",
                         "seguridad":"STARTTLS","activo":%s}
                        """.formatted(password, activo)));
    }

    private ResultActions crearAdmin(String email, boolean enviarCorreo) throws Exception {
        return mvc.perform(post("/superadmin/empresas/" + empresaId + "/usuarios").header("Authorization", superToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Dueño\",\"email\":\"" + email + "\",\"enviarCorreo\":" + enviarCorreo + "}"));
    }
}
