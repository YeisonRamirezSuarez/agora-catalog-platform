package com.agora.catalog.pedidotienda.service;

import com.agora.catalog.carro.repository.CarroRepository;
import com.agora.catalog.carro.repository.CarroStockRepository;
import com.agora.catalog.carro.service.CarroService;
import com.agora.catalog.carro.service.ReglasStock;
import com.agora.catalog.carro.service.ReglasStock.Linea;
import com.agora.catalog.carro.service.StockCarroService;
import com.agora.catalog.common.exception.ResourceNotFoundException;
import com.agora.catalog.pedidotienda.dto.PedidoTiendaDtos.LineaPedido;
import com.agora.catalog.pedidotienda.dto.PedidoTiendaDtos.PedidoRequest;
import com.agora.catalog.pedidotienda.dto.PedidoTiendaDtos.PedidoResponse;
import com.agora.catalog.pedidotienda.repository.PedidoTiendaRepository;
import com.agora.catalog.tienda.dto.TiendaDtos.TiendaResponse;
import com.agora.catalog.tienda.service.TiendaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Vender a una tienda desde un carro, todo o nada: bloquea el carro, revisa que alcance cada
 * producto, guarda el pedido con sus líneas y descuenta el stock dejando un movimiento VENTA por línea.
 */
@Service
public class CrearPedidoTiendaService {

    private final CarroRepository carros;
    private final CarroStockRepository stock;
    private final ReglasStock reglas;
    private final TiendaService tiendas;
    private final PedidoTiendaRepository pedidos;

    public CrearPedidoTiendaService(CarroRepository carros, CarroStockRepository stock, ReglasStock reglas,
                                    TiendaService tiendas, PedidoTiendaRepository pedidos) {
        this.carros = carros;
        this.stock = stock;
        this.reglas = reglas;
        this.tiendas = tiendas;
        this.pedidos = pedidos;
    }

    /** Una línea lista para guardar: validada, con su precio cobrado y su subtotal. */
    private record Venta(Linea linea, long precio, long subtotal, BigDecimal saldo) {
    }

    @Transactional
    public PedidoResponse crear(long empresaId, long usuarioId, PedidoRequest r) {
        // 1. El carro, bloqueado hasta el final: nadie más le descuenta mientras tanto.
        boolean activo = carros.lock(empresaId, r.carroId()).orElseThrow(CarroService::noExiste);
        if (!activo) throw new IllegalStateException("El carro está inactivo. Actívalo para vender desde él.");

        // 2. Las líneas validadas; un producto repetido se suma (con el precio de la primera vez).
        Map<String, Linea> lineas = new LinkedHashMap<>();
        Map<String, Long> precios = new LinkedHashMap<>();
        for (LineaPedido l : r.items()) {
            Linea linea = reglas.linea(empresaId, l.productoId(), l.presentacion(), l.cantidad(), false);
            lineas.merge(linea.clave(), linea, (a, b) -> a.con(a.cantidad().add(b.cantidad())));
            precios.putIfAbsent(linea.clave(), l.precioUnitario() != null ? l.precioUnitario() : linea.precio());
        }

        // 3. ¿Alcanza? Se juntan todos los faltantes en un solo mensaje.
        List<Venta> ventas = new ArrayList<>();
        List<String> faltan = new ArrayList<>();
        long total = 0;
        for (Linea l : lineas.values()) {
            BigDecimal hay = stock.cantidad(empresaId, r.carroId(), l.producto().id(), l.presentacion());
            if (hay.compareTo(l.cantidad()) < 0) {
                faltan.add("«" + l.nombre() + "» (" + (hay.signum() == 0 ? "no hay" : "quedan " + hay.stripTrailingZeros().toPlainString()) + ")");
                continue;
            }
            long precio = precios.get(l.clave());
            long subtotal = l.cantidad().multiply(BigDecimal.valueOf(precio)).setScale(0, RoundingMode.HALF_UP).longValueExact();
            ventas.add(new Venta(l, precio, subtotal, hay.subtract(l.cantidad())));
            total += subtotal;
        }
        if (!faltan.isEmpty()) {
            throw new IllegalStateException("No alcanza en el carro: " + String.join(", ", faltan) + ".");
        }

        // 4. La tienda: la elegida o la escrita (se crea sola si no existe).
        TiendaResponse tienda = tiendas.resolver(empresaId, r.tiendaId(), r.tiendaNombre());
        if (r.tiendaId() != null && !tienda.activa()) {
            throw new IllegalStateException("«" + tienda.nombre() + "» está inactiva. Actívala en Tiendas para venderle.");
        }

        // 5. Pedido, líneas y descuento del stock.
        String notas = r.notas() == null || r.notas().isBlank() ? null : r.notas().trim();
        long pedidoId = pedidos.insert(empresaId, pedidos.siguienteNumero(empresaId), r.carroId(), tienda.id(), total, notas, usuarioId);
        for (Venta v : ventas) {
            Linea l = v.linea();
            pedidos.insertItem(empresaId, pedidoId, l.producto().id(), l.producto().nombre(), l.presentacion(),
                    l.producto().unidadVenta(), l.cantidad(), l.precio(), v.precio(), v.subtotal());
            stock.setCantidad(empresaId, r.carroId(), l.producto().id(), l.presentacion(), v.saldo());
            stock.movimiento(empresaId, r.carroId(), usuarioId, l.producto().id(), l.producto().nombre(), l.presentacion(),
                    StockCarroService.VENTA, l.cantidad().negate(), v.saldo(), tienda.nombre(), pedidoId);
        }
        return get(empresaId, pedidoId);
    }

    public PedidoResponse get(long empresaId, long id) {
        return pedidos.findById(empresaId, id).orElseThrow(() -> new ResourceNotFoundException("El pedido no existe"));
    }
}
