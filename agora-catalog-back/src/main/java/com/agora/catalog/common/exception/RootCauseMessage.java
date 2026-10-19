package com.agora.catalog.common.exception;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mensaje de diagnóstico con toda la cadena de causas de una excepción.
 *
 * <p>{@code e.getMessage()} da solo el mensaje de afuera, y las librerías de correo lo dejan
 * genérico ("Authentication failed") mientras el motivo real ("535 Username and Password not
 * accepted") vive en la causa. Quien mira la pantalla no ve el log del servidor: necesita el motivo.
 * Se normaliza a una línea porque termina en una celda de tabla.
 */
public final class RootCauseMessage {

    private static final int MAX_LENGTH = 1000;
    private static final String SEPARATOR = " → ";

    private RootCauseMessage() {
    }

    public static String from(Throwable throwable) {
        if (throwable == null) {
            return "Error desconocido";
        }
        List<String> messages = new ArrayList<>();
        Map<Throwable, Boolean> seen = new IdentityHashMap<>();
        for (Throwable current = throwable; current != null && seen.put(current, Boolean.TRUE) == null;
             current = current.getCause()) {
            String message = describe(current);
            if (!message.isEmpty() && !messages.contains(message)) {
                messages.add(message);
            }
        }
        String joined = String.join(SEPARATOR, messages);
        return joined.length() > MAX_LENGTH ? joined.substring(0, MAX_LENGTH) + "…" : joined;
    }

    private static String describe(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return message.replaceAll("\\s+", " ").trim();
    }
}
