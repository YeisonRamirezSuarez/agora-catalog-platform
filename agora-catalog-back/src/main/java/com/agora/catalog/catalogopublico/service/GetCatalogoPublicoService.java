package com.agora.catalog.catalogopublico.service;

import com.agora.catalog.catalogopublico.dto.response.CatalogoPublicoResponse;
import com.agora.catalog.catalogopublico.dto.response.CatalogoPublicoResponse.Coleccion;
import com.agora.catalog.catalogopublico.dto.response.CatalogoPublicoResponse.Producto;
import com.agora.catalog.catalogopublico.repository.CatalogoPublicoRepository;
import com.agora.catalog.catalogopublico.repository.CatalogoPublicoRepository.ColeccionFila;
import com.agora.catalog.catalogopublico.repository.CatalogoPublicoRepository.EmpresaPublica;
import com.agora.catalog.catalogopublico.repository.CatalogoPublicoRepository.ProductoFila;
import com.agora.catalog.common.exception.ResourceNotFoundException;
import com.agora.catalog.producto.service.ReglasProducto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GetCatalogoPublicoService {

    static final String NO_DISPONIBLE = "Catálogo no disponible";

    private final CatalogoPublicoRepository repository;
    private final Clock clock;

    public GetCatalogoPublicoService(CatalogoPublicoRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Empresa inactiva o hash que no existe: el mismo 404, sin decir cuál de los dos. */
    @Transactional(readOnly = true)
    public CatalogoPublicoResponse get(String publicHash) {
        EmpresaPublica empresa = repository.findEmpresaActiva(publicHash)
                .orElseThrow(() -> new ResourceNotFoundException(NO_DISPONIBLE));
        boolean precios = empresa.negocio().mostrarPrecios();
        LocalDate hoy = LocalDate.now(clock);
        Instant ahora = clock.instant();

        List<Producto> productos = repository.findProductos(empresa.id(), empresa.mostrarAgotados()).stream()
                .map(p -> producto(p, precios, hoy, ahora))
                .toList();
        Set<Long> publicados = productos.stream().map(Producto::id).collect(Collectors.toSet());

        return new CatalogoPublicoResponse(
                empresa.negocio(),
                repository.findWhatsapps(empresa.id()),
                repository.findSellos(empresa.id()),
                repository.findHorario(empresa.id()),
                clock.getZone().getId(),
                repository.findCategoriasActivas(empresa.id()),
                repository.findFranja(empresa.id()).orElse(null),
                colecciones(repository.findColeccionesVisibles(empresa.id()), publicados),
                productos);
    }

    private static Producto producto(ProductoFila p, boolean precios, LocalDate hoy, Instant ahora) {
        boolean vigente = ReglasProducto.ofertaVigente(p.precioOferta(), p.ofertaDesde(), p.ofertaHasta(), hoy);
        return new Producto(p.id(), p.categoriaId(), p.nombre(), p.descripcion(), p.presentacion(), p.unidadVenta(),
                precios ? p.precio() : null,
                precios && vigente ? p.precioOferta() : null,
                precios ? (vigente ? p.precioOferta() : p.precio()) : null,
                precios ? vigente : null,
                precios && vigente ? ReglasProducto.descuento(p.precio(), p.precioOferta()) : null,
                p.imagenUrl(), p.imagenThumbUrl(), p.disponible(), p.destacado(),
                ReglasProducto.esNuevo(p.creadoEn(), ahora));
    }

    /** Solo los productos que el visitante puede ver; una colección sin ninguno no se muestra. */
    private static List<Coleccion> colecciones(List<ColeccionFila> filas, Set<Long> publicados) {
        Map<Long, ColeccionFila> cabeceras = new LinkedHashMap<>();
        Map<Long, List<Long>> productos = new LinkedHashMap<>();
        for (ColeccionFila fila : filas) {
            cabeceras.putIfAbsent(fila.id(), fila);
            List<Long> ids = productos.computeIfAbsent(fila.id(), k -> new ArrayList<>());
            if (fila.productoId() != null && publicados.contains(fila.productoId())) {
                ids.add(fila.productoId());
            }
        }
        return cabeceras.values().stream()
                .filter(c -> !productos.get(c.id()).isEmpty())
                .map(c -> new Coleccion(c.id(), c.nombre(), c.descripcion(), c.imagenUrl(), productos.get(c.id())))
                .toList();
    }
}
