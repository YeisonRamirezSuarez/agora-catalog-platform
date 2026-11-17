package com.agora.catalog.ruta;

import com.agora.catalog.testing.Modulos;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Venta en ruta, fase 1: tiendas, carros, su stock y el aislamiento entre empresas. */
@SpringBootTest(properties = {"settings.security.rate-limit-enabled=false",
        "settings.storage-path=${java.io.tmpdir}/agora-catalog-pruebas"})
@AutoConfigureMockMvc
@Transactional
class VentaEnRutaIntegrationTest {

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
    private long queso;
    private long jamon;
    private long cat;

    @BeforeEach
    void datos() throws Exception {
        tokenA = admin("a");
        tokenB = admin("b");
        cat = id(json(post("/admin/categorias"), tokenA, "{\"nombre\":\"Quesos\",\"activa\":true}"));
        queso = id(json(post("/admin/productos"), tokenA, producto(cat, "Q-1", "LIBRA",
                ",\"presentaciones\":[{\"nombre\":\"Media libra\",\"precio\":9000}]")));
        jamon = id(json(post("/admin/productos"), tokenA, producto(cat, "J-1", "UNIDAD", "")));
    }

    @Test
    void otraEmpresaNoVeNiTocaTiendasNiCarrosAjenos() throws Exception {
        long tienda = id(json(post("/admin/tiendas"), tokenA, tienda("Doña Rosa")));
        long carro = id(json(post("/admin/carros"), tokenA, carro("Carro 1")));
        json(post("/admin/carros/" + carro + "/cargas"), tokenA, carga(queso, null, "2")).andExpect(status().isOk());

        mvc.perform(get("/admin/tiendas/" + tienda).header("Authorization", tokenB)).andExpect(status().isNotFound());
        json(put("/admin/tiendas/" + tienda), tokenB, tienda("Otra")).andExpect(status().isNotFound());
        json(patch("/admin/tiendas/" + tienda + "/activa"), tokenB, "{\"activo\":false}").andExpect(status().isNotFound());
        mvc.perform(delete("/admin/tiendas/" + tienda).header("Authorization", tokenB)).andExpect(status().isNotFound());
        mvc.perform(get("/admin/tiendas").header("Authorization", tokenB)).andExpect(jsonPath("$.totalElements").value(0));

        mvc.perform(get("/admin/carros/" + carro).header("Authorization", tokenB)).andExpect(status().isNotFound());
        json(put("/admin/carros/" + carro), tokenB, carro("Otro")).andExpect(status().isNotFound());
        json(patch("/admin/carros/" + carro + "/activo"), tokenB, "{\"activo\":false}").andExpect(status().isNotFound());
        json(post("/admin/carros/" + carro + "/cargas"), tokenB, carga(queso, null, "1")).andExpect(status().isNotFound());
        json(post("/admin/carros/" + carro + "/ajustes"), tokenB, "{\"items\":[{\"productoId\":" + queso
                + ",\"cantidad\":0}],\"motivo\":\"x\"}").andExpect(status().isNotFound());
        json(post("/admin/carros/" + carro + "/descargue"), tokenB, "{}").andExpect(status().isNotFound());
        mvc.perform(get("/admin/carros/" + carro + "/movimientos").header("Authorization", tokenB)).andExpect(status().isNotFound());
        mvc.perform(delete("/admin/carros/" + carro).header("Authorization", tokenB)).andExpect(status().isNotFound());
        mvc.perform(get("/admin/carros").header("Authorization", tokenB)).andExpect(jsonPath("$", hasSize(0)));

        // B tampoco carga un producto de A en su propio carro.
        long carroB = id(json(post("/admin/carros"), tokenB, carro("Carro B")));
        json(post("/admin/carros/" + carroB + "/cargas"), tokenB, carga(queso, null, "1")).andExpect(status().isNotFound());

        mvc.perform(get("/admin/carros/" + carro).header("Authorization", tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock[0].cantidad").value(2));
    }

    @Test
    void elStockSeCargaAjustaYDescargaConSusMovimientos() throws Exception {
        long carro = id(json(post("/admin/carros"), tokenA, carro("Carro 1")));

        // Dos líneas del mismo producto en una carga se suman; la presentación se reconoce sin importar mayúsculas.
        json(post("/admin/carros/" + carro + "/cargas"), tokenA, """
                {"items":[{"productoId":%d,"cantidad":2.5},{"productoId":%d,"cantidad":1},
                          {"productoId":%d,"presentacion":"media LIBRA","cantidad":4},{"productoId":%d,"cantidad":10}],
                 "nota":"Salida del lunes"}
                """.formatted(queso, queso, queso, jamon))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock", hasSize(3)))
                .andExpect(jsonPath("$.productos").value(3))
                // 3,5 lb × 18.000 + 4 × 9.000 + 10 × 5.000
                .andExpect(jsonPath("$.valor").value(149000));

        json(post("/admin/carros/" + carro + "/ajustes"), tokenA, """
                {"items":[{"productoId":%d,"cantidad":8},{"productoId":%d,"presentacion":"Media libra","cantidad":0}],
                 "motivo":"Conteo de la tarde"}
                """.formatted(jamon, queso))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock", hasSize(2)));

        mvc.perform(get("/admin/carros/" + carro + "/movimientos").header("Authorization", tokenA))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.content[0].tipo").value("AJUSTE"))
                .andExpect(jsonPath("$.content[0].motivo").value("Conteo de la tarde"));
        json(post("/admin/carros/" + carro + "/ajustes"), tokenA,
                "{\"items\":[{\"productoId\":" + jamon + ",\"cantidad\":7}]}").andExpect(status().isOk())
                .andExpect(jsonPath("$.stock[?(@.productoId == " + jamon + ")].cantidad").value(7));

        json(post("/admin/carros/" + carro + "/descargue"), tokenA, "{\"nota\":\"Fin del día\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock", hasSize(0))).andExpect(jsonPath("$.valor").value(0));
        mvc.perform(get("/admin/carros/" + carro + "/movimientos").header("Authorization", tokenA))
                .andExpect(jsonPath("$.totalElements").value(8))
                .andExpect(jsonPath("$.content[0].tipo").value("DESCARGUE"))
                .andExpect(jsonPath("$.content[0].saldo").value(0));

        // Un carro inactivo no se carga.
        json(patch("/admin/carros/" + carro + "/activo"), tokenA, "{\"activo\":false}").andExpect(status().isOk());
        json(post("/admin/carros/" + carro + "/cargas"), tokenA, carga(jamon, null, "1"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("inactivo")));

        // Vacío y con historial, se elimina: desaparece y su nombre queda libre.
        mvc.perform(delete("/admin/carros/" + carro).header("Authorization", tokenA)).andExpect(status().isNoContent());
        mvc.perform(get("/admin/carros/" + carro).header("Authorization", tokenA)).andExpect(status().isNotFound());
        mvc.perform(get("/admin/carros").header("Authorization", tokenA)).andExpect(jsonPath("$", hasSize(0)));
        json(post("/admin/carros"), tokenA, carro("Carro 1")).andExpect(status().isCreated());
    }

    @Test
    void unCarroConMercanciaNoSeEliminaYAlEliminarloSusPedidosSiguen() throws Exception {
        long carro = cargado();
        String r = json(post("/admin/pedidos-tienda"), tokenA, pedido(carro, "\"tiendaNombre\":\"Doña Rosa\"", jamon, "2"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long pedido = ((Number) JsonPath.read(r, "$.id")).longValue();

        mvc.perform(delete("/admin/carros/" + carro).header("Authorization", tokenA))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(containsString("Descárgalo")));

        json(post("/admin/carros/" + carro + "/descargue"), tokenA, "{}").andExpect(status().isOk());
        mvc.perform(delete("/admin/carros/" + carro).header("Authorization", tokenA)).andExpect(status().isNoContent());

        // Ni se vende ni se carga desde él, pero el comprobante sigue diciendo de dónde salió.
        json(post("/admin/pedidos-tienda"), tokenA, pedido(carro, "\"tiendaNombre\":\"Doña Rosa\"", jamon, "1"))
                .andExpect(status().isNotFound());
        json(post("/admin/carros/" + carro + "/cargas"), tokenA, carga(jamon, null, "1")).andExpect(status().isNotFound());
        mvc.perform(get("/admin/pedidos-tienda/" + pedido).header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carroNombre").value("Carro 1"))
                .andExpect(jsonPath("$.carroEliminado").value(true));
    }

    @Test
    void lasCantidadesSeValidanSegunLaUnidad() throws Exception {
        long carro = id(json(post("/admin/carros"), tokenA, carro("Carro 1")));
        json(post("/admin/carros/" + carro + "/cargas"), tokenA, carga(jamon, null, "1.5"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("enteras")));
        json(post("/admin/carros/" + carro + "/cargas"), tokenA, carga(queso, "Media libra", "0.5"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("enteras")));
        json(post("/admin/carros/" + carro + "/cargas"), tokenA, carga(queso, null, "1.2345"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("decimales")));
        json(post("/admin/carros/" + carro + "/cargas"), tokenA, carga(queso, "Tajado", "1"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("no tiene la presentación")));
        json(post("/admin/carros/" + carro + "/cargas"), tokenA, carga(queso, null, "0"))
                .andExpect(status().isBadRequest());
        json(post("/admin/carros/" + carro + "/cargas"), tokenA, carga(queso, null, "1.25")).andExpect(status().isOk());
    }

    @Test
    void unProductoCargadoNoSeBorraYLosNombresNoSeRepiten() throws Exception {
        long carro = id(json(post("/admin/carros"), tokenA, carro("Carro 1")));
        json(post("/admin/carros/" + carro + "/cargas"), tokenA, carga(jamon, null, "3")).andExpect(status().isOk());
        mvc.perform(delete("/admin/productos/" + jamon).header("Authorization", tokenA))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(containsString("Carro 1")));

        json(post("/admin/carros"), tokenA, carro("carro 1")).andExpect(status().isConflict());
        json(post("/admin/tiendas"), tokenA, tienda("Doña Rosa")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.telefono").value("3001234567"));
        json(post("/admin/tiendas"), tokenA, tienda("doña rosa")).andExpect(status().isConflict());
        mvc.perform(get("/admin/tiendas").param("q", "dona").header("Authorization", tokenA))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void elPedidoDescuentaDelCarroYCreaLaTiendaSola() throws Exception {
        long carro = cargado();

        // Tienda escrita a mano: se crea. Precio editado en el jamón, sugerido en el queso.
        String r1 = json(post("/admin/pedidos-tienda"), tokenA, """
                {"carroId":%d,"tiendaNombre":"  Tienda   Don Pedro ","notas":"Paga el viernes",
                 "items":[{"productoId":%d,"cantidad":1.5},{"productoId":%d,"cantidad":4,"precioUnitario":4500},
                          {"productoId":%d,"presentacion":"Media libra","cantidad":2}]}
                """.formatted(carro, queso, jamon, queso))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tiendaNombre").value("Tienda Don Pedro"))
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[0].subtotal").value(27000))
                .andExpect(jsonPath("$.items[1].precioSugerido").value(5000))
                .andExpect(jsonPath("$.items[1].precioUnitario").value(4500))
                // 1,5 × 18.000 + 4 × 4.500 + 2 × 9.000
                .andExpect(jsonPath("$.total").value(63000))
                .andReturn().getResponse().getContentAsString();
        int numero = JsonPath.read(r1, "$.numero");
        long tienda = ((Number) JsonPath.read(r1, "$.tiendaId")).longValue();

        mvc.perform(get("/admin/carros/" + carro).header("Authorization", tokenA))
                .andExpect(jsonPath("$.stock[?(@.productoId == " + queso + " && @.presentacion == null)].cantidad").value(1.5))
                .andExpect(jsonPath("$.stock[?(@.productoId == " + jamon + ")].cantidad").value(6));
        mvc.perform(get("/admin/carros/" + carro + "/movimientos").header("Authorization", tokenA))
                .andExpect(jsonPath("$.content[0].tipo").value("VENTA"))
                .andExpect(jsonPath("$.content[0].motivo").value("Tienda Don Pedro"))
                .andExpect(jsonPath("$.content[0].pedidoTiendaId").value(((Number) JsonPath.read(r1, "$.id")).longValue()));

        // Con otro nombre que es el mismo (mayúsculas): no se crea otra tienda. El número sigue.
        json(post("/admin/pedidos-tienda"), tokenA, pedido(carro, "\"tiendaNombre\":\"tienda don pedro\"", jamon, "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tiendaId").value(tienda))
                .andExpect(jsonPath("$.numero").value(numero + 1));
        mvc.perform(get("/admin/tiendas").header("Authorization", tokenA))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].pedidos").value(2));
        // Una tienda con pedidos no se elimina.
        mvc.perform(delete("/admin/tiendas/" + tienda).header("Authorization", tokenA)).andExpect(status().isConflict());
    }

    @Test
    void siNoAlcanzaNoSeGuardaNada() throws Exception {
        long carro = cargado();
        json(post("/admin/pedidos-tienda"), tokenA, """
                {"carroId":%d,"tiendaNombre":"Tienda Nueva",
                 "items":[{"productoId":%d,"cantidad":11},{"productoId":%d,"presentacion":"Media libra","cantidad":5}]}
                """.formatted(carro, jamon, queso))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("quedan 10")))
                .andExpect(jsonPath("$.message").value(containsString("Media libra")));
        mvc.perform(get("/admin/carros/" + carro).header("Authorization", tokenA))
                .andExpect(jsonPath("$.stock[?(@.productoId == " + jamon + ")].cantidad").value(10));
        mvc.perform(get("/admin/tiendas").header("Authorization", tokenA)).andExpect(jsonPath("$.totalElements").value(0));

        // Sin tienda tampoco.
        json(post("/admin/pedidos-tienda"), tokenA, pedido(carro, "\"tiendaNombre\":\" \"", jamon, "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void otraEmpresaNoVendeDesdeUnCarroAjenoNiVeSusPedidos() throws Exception {
        long carro = cargado();
        String r = json(post("/admin/pedidos-tienda"), tokenA, pedido(carro, "\"tiendaNombre\":\"Doña Rosa\"", jamon, "1"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long pedidoA = ((Number) JsonPath.read(r, "$.id")).longValue();
        long tiendaA = ((Number) JsonPath.read(r, "$.tiendaId")).longValue();

        mvc.perform(get("/admin/pedidos-tienda/" + pedidoA).header("Authorization", tokenB)).andExpect(status().isNotFound());
        json(post("/admin/pedidos-tienda"), tokenB, pedido(carro, "\"tiendaNombre\":\"X\"", jamon, "1")).andExpect(status().isNotFound());
        long carroB = id(json(post("/admin/carros"), tokenB, carro("Carro B")));
        json(post("/admin/pedidos-tienda"), tokenB, pedido(carroB, "\"tiendaId\":" + tiendaA, jamon, "1")).andExpect(status().isNotFound());

        mvc.perform(get("/admin/pedidos-tienda/" + pedidoA).header("Authorization", tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tiendaNombre").value("Doña Rosa"));
    }

    @Test
    void unaPresentacionCargadaNoSeQuitaNiSeRenombra() throws Exception {
        cargado();
        json(put("/admin/productos/" + queso), tokenA, producto(cat, "Q-1", "LIBRA",
                ",\"presentaciones\":[{\"nombre\":\"1/2 libra\",\"precio\":9000}]"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(containsString("Media libra")));
        // Conservarla (aunque cambie el precio) sí se puede.
        json(put("/admin/productos/" + queso), tokenA, producto(cat, "Q-1", "LIBRA",
                ",\"presentaciones\":[{\"nombre\":\"Media libra\",\"precio\":9500}]")).andExpect(status().isOk());
    }

    @Test
    void editarUnPedidoReacomodaElCarroYConservaElNumero() throws Exception {
        long carro = cargado();
        String r = json(post("/admin/pedidos-tienda"), tokenA, """
                {"carroId":%d,"tiendaNombre":"Doña Rosa",
                 "items":[{"productoId":%d,"cantidad":4},{"productoId":%d,"presentacion":"Media libra","cantidad":2}]}
                """.formatted(carro, jamon, queso)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long pedido = ((Number) JsonPath.read(r, "$.id")).longValue();
        int numero = JsonPath.read(r, "$.numero");

        // Menos jamón, sin medias libras, con queso por libra, otra tienda y una nota.
        json(put("/admin/pedidos-tienda/" + pedido), tokenA, """
                {"tiendaNombre":"Tienda La 14","notas":"Se corrigió con la tienda",
                 "items":[{"productoId":%d,"cantidad":1},{"productoId":%d,"cantidad":1.5}]}
                """.formatted(jamon, queso))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(numero))
                .andExpect(jsonPath("$.tiendaNombre").value("Tienda La 14"))
                .andExpect(jsonPath("$.items", hasSize(2)))
                // 1 × 5.000 + 1,5 × 18.000
                .andExpect(jsonPath("$.total").value(32000))
                .andExpect(jsonPath("$.notas").value("Se corrigió con la tienda"))
                .andExpect(jsonPath("$.editadoAt").isNotEmpty());

        // Jamón: 10 - 4 + 3 = 9. Medias libras: 4 - 2 + 2 = 4. Queso: 3 - 1,5 = 1,5.
        mvc.perform(get("/admin/carros/" + carro).header("Authorization", tokenA))
                .andExpect(jsonPath("$.stock[?(@.productoId == " + jamon + ")].cantidad").value(9))
                .andExpect(jsonPath("$.stock[?(@.presentacion == 'Media libra')].cantidad").value(4))
                .andExpect(jsonPath("$.stock[?(@.productoId == " + queso + " && @.presentacion == null)].cantidad").value(1.5));
        mvc.perform(get("/admin/carros/" + carro + "/movimientos").header("Authorization", tokenA))
                .andExpect(jsonPath("$.content[?(@.tipo == 'ANULACION')]", hasSize(2)))
                .andExpect(jsonPath("$.content[?(@.tipo == 'ANULACION')].pedidoTiendaId").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is((int) pedido))));

        // Si lo que sube no alcanza, no cambia nada.
        json(put("/admin/pedidos-tienda/" + pedido), tokenA,
                "{\"tiendaNombre\":\"Tienda La 14\",\"items\":[{\"productoId\":" + jamon + ",\"cantidad\":20}]}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("solo quedan 9 más")));
        mvc.perform(get("/admin/pedidos-tienda/" + pedido).header("Authorization", tokenA)).andExpect(jsonPath("$.total").value(32000));

        // Otra empresa no lo edita.
        json(put("/admin/pedidos-tienda/" + pedido), tokenB,
                "{\"tiendaNombre\":\"X\",\"items\":[{\"productoId\":" + jamon + ",\"cantidad\":1}]}").andExpect(status().isNotFound());
    }

    /** Un carro con 3 lb de queso, 4 medias libras y 10 jamones. */
    private long cargado() throws Exception {
        long carro = id(json(post("/admin/carros"), tokenA, carro("Carro 1")));
        json(post("/admin/carros/" + carro + "/cargas"), tokenA, """
                {"items":[{"productoId":%d,"cantidad":3},{"productoId":%d,"presentacion":"Media libra","cantidad":4},
                          {"productoId":%d,"cantidad":10}]}
                """.formatted(queso, queso, jamon)).andExpect(status().isOk());
        return carro;
    }

    private static String pedido(long carro, String tienda, long producto, String cantidad) {
        return "{\"carroId\":" + carro + "," + tienda + ",\"items\":[{\"productoId\":" + producto + ",\"cantidad\":" + cantidad + "}]}";
    }

    @Test
    void soloElAdminDeLaEmpresaEntraALaVentaEnRuta() throws Exception {
        jdbc.update("INSERT INTO main.usuario (empresa_id, nombre, email, password_hash, rol) VALUES (NULL, 'S', ?, ?, 'SUPER_ADMIN')",
                "s-" + sufijo + "@prueba.local", passwordEncoder.encode(CLAVE));
        String superAdmin = login("s-" + sufijo + "@prueba.local");
        mvc.perform(get("/admin/tiendas").header("Authorization", superAdmin)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/carros").header("Authorization", superAdmin)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/pedidos-tienda/1").header("Authorization", superAdmin)).andExpect(status().isForbidden());
        mvc.perform(get("/superadmin/empresas").header("Authorization", tokenA)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/carros")).andExpect(status().isUnauthorized());
        // El token de A sí entra.
        mvc.perform(get("/admin/carros").header("Authorization", tokenA)).andExpect(status().isOk());
    }

    @Test
    void cadaRutaMuestraLoQueCargoVendioYQuedo() throws Exception {
        long carro = id(json(post("/admin/carros"), tokenA, carro("Carro rutas")));
        // Ruta 1: carga 10 jamones y vende 3.
        json(post("/admin/carros/" + carro + "/cargas"), tokenA, carga(jamon, null, "10")).andExpect(status().isOk());
        json(post("/admin/pedidos-tienda"), tokenA, pedido(carro, "\"tiendaNombre\":\"Doña Rosa\"", jamon, "3"))
                .andExpect(status().isCreated());

        // Ruta 2: lo que sobró (7) sigue en el carro y se cargan 5 más.
        json(post("/admin/carros/" + carro + "/rutas"), tokenA, "{\"llevarSobrante\":true}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock[0].cantidad").value(7));
        // Tocar el botón otra vez sin hacer nada no deja una ruta vacía.
        json(post("/admin/carros/" + carro + "/rutas"), tokenA, "{\"llevarSobrante\":true}").andExpect(status().isOk());
        json(post("/admin/carros/" + carro + "/cargas"), tokenA, carga(jamon, null, "5")).andExpect(status().isOk());

        mvc.perform(get("/admin/carros/" + carro + "/rutas").header("Authorization", tokenA))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].numero").value(2))
                .andExpect(jsonPath("$.content[0].cerradaAt").doesNotExist())
                .andExpect(jsonPath("$.content[0].items[0].traia").value(7))
                .andExpect(jsonPath("$.content[0].items[0].cargado").value(5))
                .andExpect(jsonPath("$.content[0].items[0].queda").value(12))
                .andExpect(jsonPath("$.content[1].numero").value(1))
                .andExpect(jsonPath("$.content[1].cerradaAt").exists())
                .andExpect(jsonPath("$.content[0].pedidos", hasSize(0)))
                .andExpect(jsonPath("$.content[1].pedidos", hasSize(1)))
                .andExpect(jsonPath("$.content[1].pedidos[0].tiendaNombre").value("Doña Rosa"))
                .andExpect(jsonPath("$.content[1].pedidos[0].total").value(15000))
                .andExpect(jsonPath("$.content[1].items[0].cargado").value(10))
                .andExpect(jsonPath("$.content[1].items[0].vendido").value(3))
                .andExpect(jsonPath("$.content[1].items[0].queda").value(7));

        // Ruta 3: lo que sobró no sigue: se baja y la ruta nueva arranca vacía.
        json(post("/admin/carros/" + carro + "/rutas"), tokenA, "{\"llevarSobrante\":false}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock", hasSize(0)));
        mvc.perform(get("/admin/carros/" + carro + "/rutas").header("Authorization", tokenA))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].items", hasSize(0)))
                .andExpect(jsonPath("$.content[1].items[0].quitado").value(12))
                .andExpect(jsonPath("$.content[1].items[0].queda").value(0));

        // Otra empresa ni las ve ni las empieza.
        mvc.perform(get("/admin/carros/" + carro + "/rutas").header("Authorization", tokenB)).andExpect(status().isNotFound());
        json(post("/admin/carros/" + carro + "/rutas"), tokenB, "{\"llevarSobrante\":true}").andExpect(status().isNotFound());
    }

    private ResultActions json(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                               String token, String body) throws Exception {
        return mvc.perform(request.header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String admin(String letra) throws Exception {
        long empresa = jdbc.queryForObject("INSERT INTO main.empresa (nombre, slug, public_hash) VALUES (?, ?, ?) RETURNING id",
                Long.class, "Empresa " + letra, "empresa-" + letra + "-" + sufijo, letra + sufijo.substring(1));
        Modulos.todos(jdbc, empresa);
        String email = letra + "-" + sufijo + "@prueba.local";
        jdbc.update("INSERT INTO main.usuario (empresa_id, nombre, email, password_hash, rol) VALUES (?, 'P', ?, ?, 'ADMIN_EMPRESA')",
                empresa, email, passwordEncoder.encode(CLAVE));
        return login(email);
    }

    private String login(String email) throws Exception {
        String sesion = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + CLAVE + "\"}")).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(sesion, "$.accessToken");
    }

    private static String producto(long categoria, String codigo, String unidad, String extra) {
        long precio = unidad.equals("LIBRA") ? 18000 : 5000;
        return """
                {"codigo":"%s","categoriaId":%d,"nombre":"Producto %s","descripcion":null,"presentacion":"Libra",
                 "unidadVenta":"%s","precio":%d,"precioOferta":null,"ofertaDesde":null,"ofertaHasta":null,
                 "disponible":true,"destacado":false%s}
                """.formatted(codigo, categoria, codigo, unidad, precio, extra);
    }

    private static String tienda(String nombre) {
        return """
                {"nombre":"%s","encargado":"Rosa","telefono":"300 123 4567","direccion":"Cra 1 # 2-3","barrio":"Centro",
                 "notas":null,"activa":true}
                """.formatted(nombre);
    }

    private static String carro(String nombre) {
        return "{\"nombre\":\"" + nombre + "\",\"placa\":\"abc123\",\"activo\":true}";
    }

    private static String carga(long producto, String presentacion, String cantidad) {
        return "{\"items\":[{\"productoId\":" + producto + ",\"presentacion\":" + (presentacion == null ? "null" : "\"" + presentacion + "\"")
                + ",\"cantidad\":" + cantidad + "}]}";
    }

    private static long id(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }
}
