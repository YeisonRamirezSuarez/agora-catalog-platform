package com.agora.catalog.auth.service;

import com.agora.catalog.auth.repository.RefreshTokenRepository;
import com.agora.catalog.config.JwtProperties;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Refresh tokens opacos: 32 bytes aleatorios para el cliente y su SHA-256 en la base. Cada uso lo
 * revoca y entrega uno nuevo (rotación), así un token robado sirve una sola vez.
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final JwtProperties properties;
    private final Clock clock;

    public RefreshTokenService(RefreshTokenRepository repository, JwtProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    public String issue(long usuarioId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repository.insert(usuarioId, hash(token), clock.instant().plus(Duration.ofDays(properties.getRefreshDays())));
        return token;
    }

    /** El usuario dueño del token si estaba vigente; el token queda revocado en cualquier caso. */
    public Optional<Long> consume(String token) {
        return repository.consume(hash(token), clock.instant());
    }

    public void revoke(String token) {
        repository.revoke(hash(token));
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
