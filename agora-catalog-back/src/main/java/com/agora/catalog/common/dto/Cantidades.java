package com.agora.catalog.common.dto;

import java.math.BigDecimal;

/** Cantidades de stock (numeric(12,3)) como salen en el JSON: 2.5, 10 y no 2.500 ni 1E+1. */
public final class Cantidades {

    private Cantidades() {
    }

    public static BigDecimal limpia(BigDecimal n) {
        if (n == null) return null;
        BigDecimal s = n.stripTrailingZeros();
        return s.scale() < 0 ? s.setScale(0) : s;
    }
}
