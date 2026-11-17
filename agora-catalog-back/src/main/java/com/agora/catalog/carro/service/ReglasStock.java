package com.agora.catalog.carro.service;

import com.agora.catalog.carro.repository.CarroStockRepository;
import com.agora.catalog.carro.repository.CarroStockRepository.Otra;
import com.agora.catalog.carro.repository.CarroStockRepository.ProductoCarga;
import com.agora.catalog.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Reglas de una línea de stock, iguales para cargar un carro y para venderle a una tienda: el producto
 * es de la empresa, la presentación existe y la cantidad cuadra con la unidad.
 */
@Component
public class ReglasStock {

    /** Las unidades que se venden fraccionadas; el resto (y las otras presentaciones) va en enteros. */
    private static final Set<String> FRACCIONABLES = Set.of("LIBRA", "KILO");
    public static final BigDecimal MAXIMO = new BigDecimal("999999");

    private final CarroStockRepository stock;

    public ReglasStock(CarroStockRepository stock) {
        this.stock = stock;
    }

    /**
     * Una línea ya validada.
     *
     * @param presentacion el nombre tal como está guardado en el producto, o null si es la principal
     * @param nombre       producto y presentación, para los mensajes ("Queso (Media libra)")
     * @param precio       precio de catálogo de esa presentación, sin oferta
     */
    public record Linea(ProductoCarga producto, String presentacion, String nombre, BigDecimal cantidad, long precio) {

        public String clave() {
            return producto.id() + "|" + (presentacion == null ? "" : presentacion.toLowerCase());
        }

        public Linea con(BigDecimal otra) {
            return new Linea(producto, presentacion, nombre, otra, precio);
        }
    }

    /** {@code permiteCero}: el ajuste deja un producto en cero; la carga y la venta no. */
    public Linea linea(long empresaId, long productoId, String presentacion, BigDecimal cantidad, boolean permiteCero) {
        ProductoCarga p = stock.producto(empresaId, productoId)
                .orElseThrow(() -> new ResourceNotFoundException("El producto no existe"));
        Otra otra = presentacion(p, presentacion);
        String pres = otra == null ? null : otra.nombre();
        String nombre = pres == null ? p.nombre() : p.nombre() + " (" + pres + ")";
        BigDecimal c = cantidad.stripTrailingZeros();
        if (c.signum() < 0 || (!permiteCero && c.signum() == 0)) {
            throw new IllegalArgumentException("«" + nombre + "»: la cantidad debe ser mayor que cero");
        }
        if (c.scale() > 3) {
            throw new IllegalArgumentException("«" + nombre + "»: usa hasta tres decimales");
        }
        boolean fraccionable = pres == null && FRACCIONABLES.contains(p.unidadVenta());
        if (!fraccionable && c.scale() > 0) {
            throw new IllegalArgumentException("«" + nombre + "» va en cantidades enteras");
        }
        if (c.compareTo(MAXIMO) > 0) {
            throw new IllegalArgumentException("«" + nombre + "»: la cantidad es demasiado grande");
        }
        return new Linea(p, pres, nombre, c.signum() == 0 ? BigDecimal.ZERO : c.setScale(Math.max(c.scale(), 0)),
                otra == null ? p.precio() : otra.precio());
    }

    /**
     * La otra presentación tal como está guardada en el producto, o null si es la principal (vacía o
     * con el mismo nombre que la principal). Una que el producto no tiene es un error.
     */
    private static Otra presentacion(ProductoCarga p, String pedida) {
        if (pedida == null || pedida.isBlank()) return null;
        String limpia = pedida.trim();
        if (p.presentacion() != null && p.presentacion().equalsIgnoreCase(limpia)) return null;
        return p.otras().stream().filter(o -> o.nombre().equalsIgnoreCase(limpia)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("«" + p.nombre() + "» no tiene la presentación «" + limpia + "»"));
    }
}
