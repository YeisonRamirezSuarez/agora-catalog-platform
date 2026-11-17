package com.agora.catalog.carro.service;

import com.agora.catalog.carro.dto.CarroDtos.CarroDetalleResponse;
import com.agora.catalog.carro.dto.CarroDtos.RutaResponse;
import com.agora.catalog.carro.repository.CarroRepository;
import com.agora.catalog.carro.repository.CarroRutaRepository;
import com.agora.catalog.carro.repository.CarroStockRepository;
import com.agora.catalog.carro.repository.CarroStockRepository.Fila;
import com.agora.catalog.common.dto.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Las salidas de un carro. Empezar una ruta nueva cierra la que iba; lo que sobró sigue en el carro
 * (la ruta nueva lo muestra como «traía») o se baja al local.
 */
@Service
public class RutaCarroService {

    /** Lo que una ruta trae de la anterior: no mueve el stock, solo deja constancia. */
    public static final String INICIAL = "INICIAL";

    private final CarroRepository carros;
    private final CarroStockRepository stock;
    private final CarroRutaRepository rutas;
    private final CarroService carroService;

    public RutaCarroService(CarroRepository carros, CarroStockRepository stock, CarroRutaRepository rutas,
                            CarroService carroService) {
        this.carros = carros;
        this.stock = stock;
        this.rutas = rutas;
        this.carroService = carroService;
    }

    @Transactional
    public CarroDetalleResponse nueva(long empresaId, long usuarioId, long carroId, boolean llevarSobrante) {
        boolean activo = carros.lock(empresaId, carroId).orElseThrow(CarroService::noExiste);
        if (!activo) throw new IllegalStateException("El carro está inactivo. Actívalo para salir a ruta.");

        List<Fila> sobrante = stock.filas(empresaId, carroId);
        Optional<Long> anterior = rutas.enCurso(empresaId, carroId);
        if (!llevarSobrante) {
            // Se baja en la ruta que termina: ahí queda que no siguió.
            for (Fila f : sobrante) {
                stock.setCantidad(empresaId, carroId, f.productoId(), f.presentacion(), BigDecimal.ZERO);
                stock.movimiento(empresaId, carroId, usuarioId, f.productoId(), f.productoNombre(), f.presentacion(),
                        StockCarroService.DESCARGUE, f.cantidad().negate(), BigDecimal.ZERO, "No siguió a la ruta nueva");
            }
            if (anterior.isEmpty()) anterior = rutas.enCurso(empresaId, carroId);
        }
        if (anterior.isPresent()) {
            // Una ruta en la que no pasó nada no se guarda: tocar dos veces el botón no deja rutas vacías.
            if (rutas.tuvoActividad(empresaId, anterior.get())) rutas.cerrar(empresaId, anterior.get());
            else rutas.borrarVacia(empresaId, anterior.get());
        }

        rutas.abrir(empresaId, carroId, usuarioId);
        if (llevarSobrante) {
            for (Fila f : sobrante) {
                stock.movimiento(empresaId, carroId, usuarioId, f.productoId(), f.productoNombre(), f.presentacion(),
                        INICIAL, f.cantidad(), f.cantidad(), null);
            }
        }
        return carroService.detalle(empresaId, carroId);
    }

    public PageResponse<RutaResponse> rutas(long empresaId, long carroId, int page, int size) {
        carroService.get(empresaId, carroId);
        int pagina = Math.max(page, 0), tamano = Math.min(Math.max(size, 1), 50);
        return PageResponse.of(rutas.rutas(empresaId, carroId, pagina, tamano), rutas.contar(empresaId, carroId), pagina, tamano);
    }
}
