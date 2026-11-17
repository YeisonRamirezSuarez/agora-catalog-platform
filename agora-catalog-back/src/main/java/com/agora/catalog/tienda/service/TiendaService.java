package com.agora.catalog.tienda.service;

import com.agora.catalog.common.dto.PageResponse;
import com.agora.catalog.common.exception.DuplicateResourceException;
import com.agora.catalog.common.exception.ResourceInUseException;
import com.agora.catalog.common.exception.ResourceNotFoundException;
import com.agora.catalog.tienda.dto.TiendaDtos.TiendaRequest;
import com.agora.catalog.tienda.dto.TiendaDtos.TiendaResponse;
import com.agora.catalog.tienda.repository.TiendaRepository;
import com.agora.catalog.tienda.repository.TiendaRepository.Datos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tiendas que el negocio visita en la venta en ruta, de la empresa del token. */
@Service
public class TiendaService {

    private final TiendaRepository repository;

    public TiendaService(TiendaRepository repository) {
        this.repository = repository;
    }

    public PageResponse<TiendaResponse> listar(long empresaId, String q, Boolean activa, int page, int size) {
        int pagina = Math.max(page, 0), tamano = Math.min(Math.max(size, 1), 100);
        return PageResponse.of(repository.page(empresaId, q, activa, pagina, tamano),
                repository.count(empresaId, q, activa), pagina, tamano);
    }

    public TiendaResponse get(long empresaId, long id) {
        return repository.findById(empresaId, id).orElseThrow(() -> new ResourceNotFoundException("La tienda no existe"));
    }

    @Transactional
    public TiendaResponse crear(long empresaId, TiendaRequest r) {
        Datos d = limpiar(r);
        unico(empresaId, d.nombre(), null);
        return get(empresaId, repository.insert(empresaId, d));
    }

    @Transactional
    public TiendaResponse actualizar(long empresaId, long id, TiendaRequest r) {
        get(empresaId, id);
        Datos d = limpiar(r);
        unico(empresaId, d.nombre(), id);
        repository.update(empresaId, id, d);
        return get(empresaId, id);
    }

    @Transactional
    public TiendaResponse setActiva(long empresaId, long id, boolean activa) {
        if (repository.setActiva(empresaId, id, activa) == 0) throw new ResourceNotFoundException("La tienda no existe");
        return get(empresaId, id);
    }

    /** Solo sin pedidos: el historial de ventas no se pierde. Con pedidos, se desactiva. */
    @Transactional
    public void eliminar(long empresaId, long id) {
        TiendaResponse t = get(empresaId, id);
        if (t.pedidos() > 0) {
            throw new ResourceInUseException("«" + t.nombre() + "» tiene " + t.pedidos()
                    + (t.pedidos() == 1 ? " pedido" : " pedidos") + " en el historial. Desactívala en lugar de eliminarla.");
        }
        repository.delete(empresaId, id);
    }

    /**
     * Para el pedido: la tienda elegida de la lista ({@code id}) o, si se escribió un nombre, la que
     * ya se llama así o una nueva con solo ese nombre. Los demás datos se completan después en Tiendas.
     */
    @Transactional
    public TiendaResponse resolver(long empresaId, Long id, String nombre) {
        if (id != null) return get(empresaId, id);
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("Escribe o elige la tienda");
        String limpio = nombre.trim().replaceAll("\\s+", " ");
        if (limpio.length() > 80) throw new IllegalArgumentException("El nombre de la tienda puede tener hasta 80 caracteres");
        long tiendaId = repository.findIdByNombre(empresaId, limpio)
                .orElseGet(() -> repository.insert(empresaId, new Datos(limpio, null, null, null, null, null, true)));
        return get(empresaId, tiendaId);
    }

    private void unico(long empresaId, String nombre, Long exceptId) {
        if (repository.existsByNombre(empresaId, nombre, exceptId)) {
            throw new DuplicateResourceException("Ya tienes una tienda llamada «" + nombre + "»");
        }
    }

    private static Datos limpiar(TiendaRequest r) {
        String telefono = r.telefono() == null ? null : r.telefono().replaceAll("\\D", "");
        if (telefono != null && !telefono.isEmpty() && (telefono.length() < 7 || telefono.length() > 15)) {
            throw new IllegalArgumentException("El teléfono debe tener entre 7 y 15 dígitos");
        }
        return new Datos(r.nombre().trim(), vacioANull(r.encargado()), vacioANull(telefono), vacioANull(r.direccion()),
                vacioANull(r.barrio()), vacioANull(r.notas()), r.activa());
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
