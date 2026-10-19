package com.agora.catalog.producto.service;

import com.agora.catalog.common.dto.PageResponse;
import com.agora.catalog.common.exception.DuplicateResourceException;
import com.agora.catalog.common.exception.ResourceNotFoundException;
import com.agora.catalog.producto.dto.ProductoDtos.CambioPrecio;
import com.agora.catalog.producto.dto.ProductoDtos.ProductoRequest;
import com.agora.catalog.producto.dto.ProductoDtos.ProductoResponse;
import com.agora.catalog.producto.repository.ProductoRepository;
import com.agora.catalog.producto.repository.ProductoRepository.Fila;
import com.agora.catalog.storage.ImagenService;
import com.agora.catalog.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** Productos de la empresa del token. */
@Service
public class ProductoService {

    private final ProductoRepository repository;
    private final ImagenService imagenes;
    private final StorageService storage;
    private final Clock clock;

    public ProductoService(ProductoRepository repository, ImagenService imagenes, StorageService storage, Clock clock) {
        this.repository = repository;
        this.imagenes = imagenes;
        this.storage = storage;
        this.clock = clock;
    }

    public PageResponse<ProductoResponse> listar(long empresaId, ProductoRepository.Filtro filtro, int page, int size) {
        int pagina = Math.max(page, 0), tamano = Math.min(Math.max(size, 1), 100);
        LocalDate hoy = LocalDate.now(clock);
        List<ProductoResponse> contenido = repository.page(empresaId, filtro, pagina, tamano).stream()
                .map(f -> respuesta(f, hoy)).toList();
        return PageResponse.of(contenido, repository.count(empresaId, filtro), pagina, tamano);
    }

    public ProductoResponse get(long empresaId, long id) {
        return respuesta(fila(empresaId, id), LocalDate.now(clock));
    }

    @Transactional
    public ProductoResponse crear(long empresaId, ProductoRequest r) {
        validar(empresaId, r, null);
        return get(empresaId, repository.insert(empresaId, r));
    }

    @Transactional
    public ProductoResponse actualizar(long empresaId, long id, ProductoRequest r) {
        fila(empresaId, id);
        validar(empresaId, r, id);
        repository.update(empresaId, id, r);
        return get(empresaId, id);
    }

    @Transactional
    public void eliminar(long empresaId, long id) {
        Fila f = fila(empresaId, id);
        repository.delete(empresaId, id); // coleccion_producto se borra en cascada
        storage.borrar(f.imagenUrl());
        storage.borrar(f.imagenThumbUrl());
    }

    @Transactional
    public ProductoResponse setDisponible(long empresaId, long id, boolean disponible) {
        if (repository.setDisponible(empresaId, id, disponible) == 0) throw noExiste();
        return get(empresaId, id);
    }

    /** Edición rápida de precios del listado: todo o nada, para no dejar la mitad guardada. */
    @Transactional
    public void cambiarPrecios(long empresaId, List<CambioPrecio> cambios) {
        for (CambioPrecio c : cambios) {
            Fila f = fila(empresaId, c.id());
            if (f.precioOferta() != null && c.precio() <= f.precioOferta().longValue()) {
                throw new IllegalArgumentException("«" + f.nombre() + "» tiene una oferta de $" + f.precioOferta().toPlainString()
                        + ": el precio debe ser mayor. Cambia la oferta desde el producto.");
            }
            repository.setPrecio(empresaId, c.id(), c.precio());
        }
    }

    /** Foto nueva: grande y miniatura en JPG; la anterior se borra después de guardar la nueva. */
    @Transactional
    public ProductoResponse subirImagen(long empresaId, long id, byte[] archivo) {
        Fila f = fila(empresaId, id);
        ImagenService.FotoProducto foto = imagenes.producto(archivo);
        String grande = storage.guardar(empresaId, "productos", foto.grande());
        String miniatura = storage.guardar(empresaId, "productos", foto.miniatura());
        repository.setImagen(empresaId, id, grande, miniatura);
        storage.borrar(f.imagenUrl());
        storage.borrar(f.imagenThumbUrl());
        return get(empresaId, id);
    }

    @Transactional
    public ProductoResponse quitarImagen(long empresaId, long id) {
        Fila f = fila(empresaId, id);
        repository.setImagen(empresaId, id, null, null);
        storage.borrar(f.imagenUrl());
        storage.borrar(f.imagenThumbUrl());
        return get(empresaId, id);
    }

    private void validar(long empresaId, ProductoRequest r, Long exceptId) {
        List<String> errores = ValidarProducto.errores(r);
        if (!errores.isEmpty()) throw new IllegalArgumentException(String.join(". ", errores));
        if (!repository.categoriaDeLaEmpresa(empresaId, r.categoriaId())) {
            throw new ResourceNotFoundException("La categoría no existe");
        }
        if (repository.existsByCodigo(empresaId, r.codigo().trim(), exceptId)) {
            throw new DuplicateResourceException("Ya tienes un producto con el código " + r.codigo().trim());
        }
    }

    private Fila fila(long empresaId, long id) {
        return repository.findById(empresaId, id).orElseThrow(ProductoService::noExiste);
    }

    private static ResourceNotFoundException noExiste() {
        return new ResourceNotFoundException("El producto no existe");
    }

    private static ProductoResponse respuesta(Fila f, LocalDate hoy) {
        return new ProductoResponse(f.id(), f.codigo(), f.categoriaId(), f.categoriaNombre(), f.nombre(), f.descripcion(),
                f.presentacion(), f.unidadVenta(), f.precio(), f.precioOferta(), f.ofertaDesde(), f.ofertaHasta(),
                ReglasProducto.ofertaVigente(f.precioOferta(), f.ofertaDesde(), f.ofertaHasta(), hoy),
                f.imagenUrl(), f.imagenThumbUrl(), f.disponible(), f.destacado(), f.orden());
    }
}
