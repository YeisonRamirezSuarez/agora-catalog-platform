package com.agora.catalog.empresa.service;

/**
 * Contraste entre dos colores según WCAG 2.1. El catálogo exige 4,5:1: el color de botones con texto
 * blanco, y el de acento sobre la portada noche ({@link #NOCHE}).
 */
public final class Contraste {

    public static final String NOCHE = "#15120D";
    public static final double MINIMO = 4.5;

    private Contraste() {
    }

    public static double ratio(String a, String b) {
        double la = luminancia(a), lb = luminancia(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminancia(String hex) {
        int rgb = Integer.parseInt(hex.substring(1), 16);
        return 0.2126 * canal((rgb >> 16) & 0xFF) + 0.7152 * canal((rgb >> 8) & 0xFF) + 0.0722 * canal(rgb & 0xFF);
    }

    private static double canal(int c) {
        double s = c / 255.0;
        return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }
}
