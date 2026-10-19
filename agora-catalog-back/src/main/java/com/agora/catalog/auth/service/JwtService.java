package com.agora.catalog.auth.service;

import com.agora.catalog.auth.model.UsuarioPrincipal;
import com.agora.catalog.config.JwtProperties;
import com.agora.catalog.usuario.model.Rol;
import com.agora.catalog.usuario.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

/**
 * Token de acceso corto y sin estado: lleva el id, el rol y la empresa del usuario, así que el
 * filtro no consulta la base en cada petición. El precio es que desactivar a alguien corta su
 * acceso cuando vence el token ({@code config.jwt.access-minutes}), no en el acto.
 */
@Service
public class JwtService {

    private static final String ROL_CLAIM = "rol";
    private static final String EMPRESA_CLAIM = "empresa_id";

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey key;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.getSecret()));
    }

    public String generateAccessToken(Usuario usuario) {
        Instant now = clock.instant();
        var builder = Jwts.builder()
                .subject(String.valueOf(usuario.id()))
                .claim(ROL_CLAIM, usuario.rol().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessSeconds())))
                .signWith(key);
        if (usuario.empresaId() != null) {
            builder.claim(EMPRESA_CLAIM, usuario.empresaId());
        }
        return builder.compact();
    }

    /** @throws JwtException si la firma no es válida, el token venció o no es un JWT */
    public UsuarioPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Number empresaId = claims.get(EMPRESA_CLAIM, Number.class);
        return new UsuarioPrincipal(
                Long.parseLong(claims.getSubject()),
                empresaId == null ? null : empresaId.longValue(),
                Rol.valueOf(claims.get(ROL_CLAIM, String.class)));
    }

    public long accessSeconds() {
        return properties.getAccessMinutes() * 60L;
    }
}
