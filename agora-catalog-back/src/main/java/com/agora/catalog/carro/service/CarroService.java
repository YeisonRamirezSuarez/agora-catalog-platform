package com.agora.catalog.carro.service;

import com.agora.catalog.carro.dto.CarroDtos.CarroDetalleResponse;
import com.agora.catalog.carro.dto.CarroDtos.CarroRequest;
import com.agora.catalog.carro.dto.CarroDtos.CarroResponse;
import com.agora.catalog.carro.repository.CarroRepository;
import com.agora.catalog.carro.repository.CarroStockRepository;
import com.agora.catalog.common.exception.DuplicateResourceException;
import com.agora.catalog.common.exception.ResourceInUseException;
import com.agora.catalog.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Carros de la venta en ruta de la empresa del token: crear, renombrar, activar y ver qué llevan. */
@Service
public class CarroService {

    private final CarroRepository repository;
    private final CarroStockRepository stock;

    public CarroService(CarroRepository repository, CarroStockRepository stock) {
        this.repository = repository;
        this.stock = stock;
    }

    public List<CarroResponse> list(long empresaId) {
        return repository.findAll(empresaId);
    }

    public CarroResponse get(long empresaId, long id) {
        return repository.findById(empresaId, id).orElseThrow(CarroService::noExiste);
    }

    public CarroDetalleResponse detalle(long empresaId, long id) {
        CarroResponse c = get(empresaId, id);
        return new CarroDetalleResponse(c.id(), c.nombre(), c.placa(), c.activo(), c.productos(), c.valor(),
                stock.stock(empresaId, id));
    }

    @Transactional
    public CarroDetalleResponse crear(long empresaId, CarroRequest r) {
        String nombre = r.nombre().trim();
        unico(empresaId, nombre, null);
        return detalle(empresaId, repository.insert(empresaId, nombre, vacioANull(r.placa()), r.activo()));
    }

    @Transactional
    public CarroDetalleResponse actualizar(long empresaId, long id, CarroRequest r) {
        get(empresaId, id);
        String nombre = r.nombre().trim();
        unico(empresaId, nombre, id);
        repository.update(empresaId, id, nombre, vacioANull(r.placa()), r.activo());
        return detalle(empresaId, id);
    }

    @Transactional
    public CarroResponse setActivo(long empresaId, long id, boolean activo) {
        if (repository.setActivo(empresaId, id, activo) == 0) throw noExiste();
        return get(empresaId, id);
    }

    /**
     * Vacío, se elimina. Sin historial se borra del todo; con movimientos o pedidos se marca como
     * eliminado: desaparece del panel y sus comprobantes siguen diciendo de qué carro salieron.
     * Con mercancía no: primero se descarga, para que el stock no se pierda sin registro.
     */
    @Transactional
    public void eliminar(long empresaId, long id) {
        repository.lock(empresaId, id).orElseThrow(CarroService::noExiste);
        CarroResponse c = get(empresaId, id);
        if (c.productos() > 0) {
            throw new ResourceInUseException("«" + c.nombre() + "» todavía lleva " + c.productos()
                    + (c.productos() == 1 ? " producto" : " productos") + ". Descárgalo antes de eliminarlo.");
        }
        if (repository.historial(empresaId, id) > 0) repository.marcarEliminado(empresaId, id);
        else repository.delete(empresaId, id);
    }

    private void unico(long empresaId, String nombre, Long exceptId) {
        if (repository.existsByNombre(empresaId, nombre, exceptId)) {
            throw new DuplicateResourceException("Ya tienes un carro llamado «" + nombre + "»");
        }
    }

    public static ResourceNotFoundException noExiste() {
        return new ResourceNotFoundException("El carro no existe");
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim().toUpperCase();
    }
}
