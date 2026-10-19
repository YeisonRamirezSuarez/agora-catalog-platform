package com.agora.catalog.producto.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ReglasProductoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final BigDecimal OFERTA = BigDecimal.valueOf(9_000);

    @Test
    void laOfertaEsVigenteEntreSusFechasIncluidosLosExtremos() {
        assertThat(ReglasProducto.ofertaVigente(OFERTA, HOY, HOY, HOY)).isTrue();
        assertThat(ReglasProducto.ofertaVigente(OFERTA, HOY.minusDays(3), HOY.plusDays(3), HOY)).isTrue();
        assertThat(ReglasProducto.ofertaVigente(OFERTA, HOY.plusDays(1), null, HOY)).isFalse();
        assertThat(ReglasProducto.ofertaVigente(OFERTA, null, HOY.minusDays(1), HOY)).isFalse();
    }

    @Test
    void sinFechasLaOfertaQuedaAbiertaYSinPrecioDeOfertaNoHayOferta() {
        assertThat(ReglasProducto.ofertaVigente(OFERTA, null, null, HOY)).isTrue();
        assertThat(ReglasProducto.ofertaVigente(null, null, null, HOY)).isFalse();
    }

    @Test
    void esNuevoDuranteTreintaDias() {
        Instant ahora = Instant.parse("2026-10-19T15:00:00Z");
        assertThat(ReglasProducto.esNuevo(ahora.minus(29, ChronoUnit.DAYS), ahora)).isTrue();
        assertThat(ReglasProducto.esNuevo(ahora.minus(30, ChronoUnit.DAYS), ahora)).isFalse();
    }

    @Test
    void elDescuentoSeRedondeaAlEntero() {
        assertThat(ReglasProducto.descuento(BigDecimal.valueOf(16_800), BigDecimal.valueOf(14_900))).isEqualTo(11);
        assertThat(ReglasProducto.descuento(BigDecimal.valueOf(10_000), BigDecimal.valueOf(7_500))).isEqualTo(25);
        assertThat(ReglasProducto.descuento(BigDecimal.ZERO, OFERTA)).isZero();
    }
}
