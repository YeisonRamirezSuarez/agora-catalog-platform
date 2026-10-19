package com.agora.catalog.common.encryption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifra secretos que la aplicación necesita leer de vuelta (la contraseña del proveedor de correo),
 * con AES-GCM: además de ocultar el valor, detecta si alguien lo alteró en la base.
 *
 * <p>Formato guardado: base64(iv de 12 bytes + texto cifrado con su etiqueta).
 */
@Component
public class AesEncryption {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec key;

    public AesEncryption(@Value("${settings.encryption.aes.key}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Falta AES_KEY: sin ella no se pueden guardar secretos cifrados");
        }
        try {
            // Cualquier largo de AES_KEY sirve: SHA-256 la lleva a los 32 bytes de AES-256.
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(bytes, "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv).put(encrypted).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el valor", e);
        }
    }

    /** @throws IllegalStateException si el valor fue alterado o se cifró con otra AES_KEY */
    public String decrypt(String stored) {
        try {
            byte[] data = Base64.getDecoder().decode(stored);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, data, 0, IV_BYTES));
            return new String(cipher.doFinal(data, IV_BYTES, data.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException(
                    "No se pudo leer un secreto guardado: ¿cambió AES_KEY? Vuelve a escribir la contraseña", e);
        }
    }
}
