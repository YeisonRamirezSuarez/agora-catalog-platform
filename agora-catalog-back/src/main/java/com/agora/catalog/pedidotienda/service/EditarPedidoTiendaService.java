package com.agora.catalog.pedidotienda.service;

import com.agora.catalog.carro.repository.CarroRepository;
import com.agora.catalog.carro.repository.CarroStockRepository;
import com.agora.catalog.carro.service.ReglasStock;
import com.agora.catalog.carro.service.ReglasStock.Linea;
import com.agora.catalog.carro.service.StockCarroService;
import com.agora.catalog.common.exception.ResourceNotFoundException;
import com.agora.catalog.pedidotienda.dto.PedidoTiendaDtos.EditarPedidoRequest;
import com.agora.catalog.pedidotienda.dto.PedidoTiendaDtos.LineaPedido;
import com.agora.catalog.pedidotienda.dto.PedidoTiendaDtos.PedidoResponse;
import com.agora.catalog.pedidotienda.repository.PedidoTiendaRepository;
import com.agora.catalog.pedidotienda.repository.PedidoTiendaRepository.Cabecera;
import com.agora.catalog.pedidotienda.repository.PedidoTiendaRepository.LineaGuardada;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Corregir un pedido ya hecho, todo o nada. Por cada producto se compara lo que tenía con lo que
 * tiene ahora: si bajó, la diferencia vuelve al carro (movimiento ANULACION, "Devuelto"); si subió,
 * se descuenta del carro (VENTA) y tiene que alcanzar. El número del pedido no cambia.
 */
@Service
public class EditarPedidoTiendaService {

    private final PedidoTiendaRepository pedidos;
    private final CarroRepository carros;
    private final CarroStockRepository stock;
    private final ReglasStock reglas;
    private final CrearPedidoTiendaService crear;

    public EditarPedidoTiendaService(PedidoTiendaRepository pedidos, CarroRepository carros, CarroStockRepository stock,
                                     ReglasStock reglas, CrearPedidoTiendaService crear) {
        this.pedidos = pedidos;
        this.carros = carros;
        this.stock = stock;
        this.reglas = reglas;
        this.crear = crear;
    }

    @Transactional
    public PedidoResponse editar(long empresaId, long usuarioId, long pedidoId, EditarPedidoRequest r) {
        // 1. Primero el carro y después el pedido (el mismo orden que al crear): dos ediciones van una tras otra.
        long carroId = pedidos.carroDe(empresaId, pedidoId).orElseThrow(EditarPedidoTiendaService::noExiste);
        if (carros.lock(empresaId, carroId).isEmpty()) {
            throw new IllegalStateException("El carro de este pedido ya se eliminó: el pedido no se puede editar.");
        }
        Cabecera pedido = pedidos.lock(empresaId, pedidoId).orElseThrow(EditarPedidoTiendaService::noExiste);
        if (!"CONFIRMADO".equals(pedido.estado())) throw new IllegalStateException("Un pedido anulado no se puede editar.");

        // 2. Lo nuevo, validado; un producto repetido se suma (con el precio de la primera vez).
        Map<String, Linea> nuevas = new LinkedHashMap<>();
        Map<String, Long> precios = new HashMap<>();
        for (LineaPedido l : r.items()) {
            Linea linea = reglas.linea(empresaId, l.productoId(), l.presentacion(), l.cantidad(), false);
            nuevas.merge(linea.clave(), linea, (a, b) -> a.con(a.cantidad().add(b.cantidad())));
            precios.putIfAbsent(linea.clave(), l.precioUnitario() != null ? l.precioUnitario() : linea.precio());
        }

        // 3. Lo que tenía. Las líneas de productos que ya no están en el catálogo se conservan tal cual.
        Map<String, BigDecimal> antes = new HashMap<>();
        Map<String, LineaGuardada> guardadas = new HashMap<>();
        long totalConservado = 0;
        for (LineaGuardada g : pedidos.lineas(empresaId, pedidoId)) {
            if (g.productoId() == null) {
                totalConservado += g.subtotal();
                continue;
            }
            antes.merge(clave(g.productoId(), g.presentacion()), g.cantidad(), BigDecimal::add);
            guardadas.putIfAbsent(clave(g.productoId(), g.presentacion()), g);
        }

        // 4. ¿Alcanza lo que sube? Se juntan todos los faltantes en un solo mensaje.
        Set<String> claves = new LinkedHashSet<>(nuevas.keySet());
        claves.addAll(antes.keySet());
        List<String> faltan = new ArrayList<>();
        for (String k : nuevas.keySet()) {
            Linea l = nuevas.get(k);
            BigDecimal extra = l.cantidad().subtract(antes.getOrDefault(k, BigDecimal.ZERO));
            if (extra.signum() <= 0) continue;
            BigDecimal hay = stock.cantidad(empresaId, carroId, l.producto().id(), l.presentacion());
            if (hay.compareTo(extra) < 0) {
                faltan.add("«" + l.nombre() + "» (" + (hay.signum() == 0 ? "no queda más" : "solo quedan " + hay.stripTrailingZeros().toPlainString() + " más") + ")");
            }
        }
        if (!faltan.isEmpty()) throw new IllegalStateException("No alcanza en el carro: " + String.join(", ", faltan) + ".");

        // 5. La tienda (puede cambiar) y las líneas nuevas.
        String tienda = crear.nombreTienda(empresaId, r.tiendaNombre());
        String motivo = tienda + " · corrección del pedido";
        pedidos.borrarLineasConProducto(empresaId, pedidoId);
        long total = totalConservado;
        for (Linea l : nuevas.values()) {
            long precio = precios.get(l.clave());
            long subtotal = l.cantidad().multiply(BigDecimal.valueOf(precio)).setScale(0, RoundingMode.HALF_UP).longValueExact();
            pedidos.insertItem(empresaId, pedidoId, l.producto().id(), l.producto().nombre(), l.presentacion(),
                    l.producto().unidadVenta(), l.cantidad(), l.precio(), precio, subtotal);
            total += subtotal;
        }

        // 6. El carro: cada diferencia sale o vuelve, con su movimiento ligado al pedido.
        for (String k : claves) {
            Linea nueva = nuevas.get(k);
            BigDecimal ahora = nueva == null ? BigDecimal.ZERO : nueva.cantidad();
            BigDecimal diferencia = antes.getOrDefault(k, BigDecimal.ZERO).subtract(ahora); // + vuelve al carro, - sale
            if (diferencia.signum() == 0) continue;
            // Una línea que se quitó del pedido usa la presentación tal como estaba guardada.
            long productoId = nueva != null ? nueva.producto().id() : guardadas.get(k).productoId();
            String presentacion = nueva != null ? nueva.presentacion() : guardadas.get(k).presentacion();
            String nombre = nueva != null ? nueva.producto().nombre()
                    : stock.producto(empresaId, productoId).map(p -> p.nombre()).orElse("Producto");
            BigDecimal saldo = stock.cantidad(empresaId, carroId, productoId, presentacion).add(diferencia);
            stock.setCantidad(empresaId, carroId, productoId, presentacion, saldo);
            stock.movimiento(empresaId, carroId, usuarioId, productoId, nombre, presentacion,
                    diferencia.signum() > 0 ? StockCarroService.ANULACION : StockCarroService.VENTA, diferencia, saldo, motivo, pedidoId);
        }

        String notas = r.notas() == null || r.notas().isBlank() ? null : r.notas().trim();
        pedidos.actualizar(empresaId, pedidoId, tienda, total, notas);
        return crear.get(empresaId, pedidoId);
    }

    static String clave(long productoId, String presentacion) {
        return productoId + "|" + (presentacion == null ? "" : presentacion.toLowerCase());
    }

    private static ResourceNotFoundException noExiste() {
        return new ResourceNotFoundException("El pedido no existe");
    }
}
