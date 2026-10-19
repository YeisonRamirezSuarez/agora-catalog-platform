package com.agora.catalog.common.random;

import java.security.SecureRandom;

/** Códigos aleatorios con {@link SecureRandom}: hash público de la empresa y contraseñas temporales. */
public final class RandomCodes {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String HASH_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    // Sin 0/O, 1/l/I: la contraseña temporal se dicta o se copia a mano.
    private static final String PASSWORD_ALPHABET = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private RandomCodes() {
    }

    /** 8 caracteres [a-z0-9]: lo que identifica el catálogo en el link público. */
    public static String publicHash() {
        return random(HASH_ALPHABET, 8);
    }

    /** 12 caracteres legibles; se pide cambiarla en el primer ingreso. */
    public static String temporaryPassword() {
        return random(PASSWORD_ALPHABET, 12);
    }

    private static String random(String alphabet, int length) {
        StringBuilder value = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            value.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return value.toString();
    }
}
