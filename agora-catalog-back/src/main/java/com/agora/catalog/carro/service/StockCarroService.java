package com.agora.catalog.carro.service;

import com.agora.catalog.carro.dto.CarroDtos.AjusteRequest;
import com.agora.catalog.carro.dto.CarroDtos.CargaRequest;
import com.agora.catalog.carro.dto.CarroDtos.CarroDetalleResponse;
import com.agora.catalog.carro.dto.CarroDtos.LineaAjuste;
import com.agora.catalog.carro.dto.CarroDtos.LineaCarga;
import com.agora.catalog.carro.dto.CarroDtos.MovimientoResponse;
import com.agora.catalog.carro.repository.CarroRepository;
import com.agora.catalog.carro.repository.CarroStockRepository;
import com.agora.catalog.carro.repository.CarroStockRepository.Fila;
import com.agora.catalog.carro.service.ReglasStock.Linea;
import com.agora.catalog.common.dto.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Entradas y salidas de un carro: cargar, ajustar a lo contado y descargar lo que sobró. Cada una
 * bloquea el carro, cambia el stock y deja un movimiento con el saldo que quedó.
 */
@Service
public class StockCarroService {

    public static final String CARGA = "CARGA";
    public static final String VENTA = "VENTA";
    public static final String AJUSTE = "AJUSTE";
    public static final String DESCARGUE = "DESCARGUE";
    /** Producto que vuelve al carro porque se quitó de un pedido. */
    public static final String ANULACION = "ANULACION";

    private final CarroRepository carros;
    private final CarroStockRepository stock;
    private final CarroService carroService;
    private final ReglasStock reglas;

    public StockCarroService(CarroRepository carros, CarroStockRepository stock, CarroService carroService, ReglasStock reglas) {
        this.carros = carros;
        this.stock = stock;
        this.carroService = carroService;
        this.reglas = reglas;
    }

    /** Suma al carro. Si el mismo producto viene dos veces en la carga, se suman las dos. */
    @Transactional
    public CarroDetalleResponse cargar(long empresaId, long usuarioId, long carroId, CargaRequest r) {
        if (!bloquear(empresaId, carroId)) {
            throw new IllegalStateException("El carro está inactivo. Actívalo para cargarlo.");
        }
        Map<String, Linea> lineas = new LinkedHashMap<>();
        for (LineaCarga l : r.items()) {
            Linea linea = reglas.linea(empresaId, l.productoId(), l.presentacion(), l.cantidad(), false);
            lineas.merge(linea.clave(), linea, (a, b) -> a.con(a.cantidad().add(b.cantidad())));
        }
        String nota = vacioANull(r.nota());
        for (Linea l : lineas.values()) {
            BigDecimal saldo = stock.cantidad(empresaId, carroId, l.producto().id(), l.presentacion()).add(l.cantidad());
            if (saldo.compareTo(ReglasStock.MAXIMO) > 0) {
                throw new IllegalArgumentException("Con esta carga «" + l.nombre() + "» pasaría de " + ReglasStock.MAXIMO.toPlainString());
            }
            stock.setCantidad(empresaId, carroId, l.producto().id(), l.presentacion(), saldo);
            stock.movimiento(empresaId, carroId, usuarioId, l.producto().id(), l.producto().nombre(), l.presentacion(),
                    CARGA, l.cantidad(), saldo, nota);
        }
        return carroService.detalle(empresaId, carroId);
    }

    /** Deja cada producto en la cantidad indicada (sumar, restar o quitar) y registra la diferencia. */
    @Transactional
    public CarroDetalleResponse ajustar(long empresaId, long usuarioId, long carroId, AjusteRequest r) {
        bloquear(empresaId, carroId);
        Set<String> vistos = new HashSet<>();
        String motivo = vacioANull(r.motivo());
        for (LineaAjuste l : r.items()) {
            Linea linea = reglas.linea(empresaId, l.productoId(), l.presentacion(), l.cantidad(), true);
            if (!vistos.add(linea.clave())) {
                throw new IllegalArgumentException("«" + linea.nombre() + "» viene dos veces en el ajuste");
            }
            BigDecimal actual = stock.cantidad(empresaId, carroId, linea.producto().id(), linea.presentacion());
            BigDecimal diferencia = linea.cantidad().subtract(actual);
            if (diferencia.signum() == 0) continue;
            stock.setCantidad(empresaId, carroId, linea.producto().id(), linea.presentacion(), linea.cantidad());
            stock.movimiento(empresaId, carroId, usuarioId, linea.producto().id(), linea.producto().nombre(),
                    linea.presentacion(), AJUSTE, diferencia, linea.cantidad(), motivo);
        }
        return carroService.detalle(empresaId, carroId);
    }

    /** Vacía el carro (lo que sobró vuelve al local). */
    @Transactional
    public CarroDetalleResponse descargar(long empresaId, long usuarioId, long carroId, String nota) {
        bloquear(empresaId, carroId);
        String limpia = vacioANull(nota);
        for (Fila f : stock.filas(empresaId, carroId)) {
            stock.setCantidad(empresaId, carroId, f.productoId(), f.presentacion(), BigDecimal.ZERO);
            stock.movimiento(empresaId, carroId, usuarioId, f.productoId(), f.productoNombre(), f.presentacion(),
                    DESCARGUE, f.cantidad().negate(), BigDecimal.ZERO, limpia);
        }
        return carroService.detalle(empresaId, carroId);
    }

    public PageResponse<MovimientoResponse> movimientos(long empresaId, long carroId, int page, int size) {
        carroService.get(empresaId, carroId);
        int pagina = Math.max(page, 0), tamano = Math.min(Math.max(size, 1), 100);
        return PageResponse.of(stock.movimientos(empresaId, carroId, pagina, tamano),
                stock.contarMovimientos(empresaId, carroId), pagina, tamano);
    }

    /** Bloquea el carro para el resto de la transacción y dice si está activo. */
    private boolean bloquear(long empresaId, long carroId) {
        return carros.lock(empresaId, carroId).orElseThrow(CarroService::noExiste);
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
