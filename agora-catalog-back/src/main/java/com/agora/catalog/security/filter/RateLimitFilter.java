package com.agora.catalog.security.filter;

import com.agora.catalog.common.net.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Límite de peticiones por IP sobre lo que cualquiera puede llamar sin token.
 *
 * <p>Dos cupos separados, porque protegen cosas distintas:
 * <ul>
 *   <li><b>Autenticación</b> ({@code /auth/login}, {@code /auth/refresh}): estricto, frena el
 *       ataque de fuerza bruta desde un origen.</li>
 *   <li><b>Catálogo público</b> ({@code /public/}): amplio, porque los operadores móviles
 *       comparten una IP entre muchos clientes (CGNAT) y un cupo corto dejaría sin catálogo a
 *       visitantes reales.</li>
 * </ul>
 *
 * <p>La IP sale de {@link ClientIpResolver}: detrás de Caddy, sin él, todos los visitantes
 * comparten un solo cupo.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // shortcut: contador en memoria por instancia; con dos instancias o balanceador, pasar a un almacén compartido.

    enum Quota {
        AUTH(10, 60),
        PUBLIC(120, 60);

        final int maxRequests;
        final long windowNanos;

        Quota(int maxRequests, int windowSeconds) {
            this.maxRequests = maxRequests;
            this.windowNanos = windowSeconds * 1_000_000_000L;
        }
    }

    /** Techo del mapa. Sin esto, un ataque desde IP cambiantes lo hace crecer sin fin. */
    private static final int MAX_TRACKED_IDENTITIES = 50_000;

    private final ClientIpResolver clientIpResolver;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(ClientIpResolver clientIpResolver) {
        this.clientIpResolver = clientIpResolver;
    }

    /** El cupo que le toca a la ruta, o null si la ruta no se limita (ya exige token). */
    static Quota quotaFor(String path) {
        if (path.startsWith("/auth/login") || path.startsWith("/auth/refresh")) return Quota.AUTH;
        if (path.startsWith("/public/")) return Quota.PUBLIC;
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // El preflight de CORS no lo manda una persona: contarlo gastaría el cupo del navegador
        // antes de la petición real.
        return "OPTIONS".equalsIgnoreCase(request.getMethod()) || quotaFor(pathOf(request)) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        Quota quota = quotaFor(pathOf(request));
        String identity = quota.name() + ':' + clientIpResolver.resolve(request);
        long now = System.nanoTime();

        if (windows.size() > MAX_TRACKED_IDENTITIES) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt > entry.getValue().quota.windowNanos);
        }

        Window window = windows.compute(identity, (key, existing) -> {
            if (existing == null || now - existing.startedAt > quota.windowNanos) {
                return new Window(now, quota);
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (window.count.get() > quota.maxRequests) {
            long retryAfter = Math.max(1, (quota.windowNanos - (now - window.startedAt)) / 1_000_000_000L);
            log.warn("[RATE] {} superó el límite en {} ({} peticiones); faltan {}s",
                    identity, request.getRequestURI(), window.count.get(), retryAfter);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            // El mismo cuerpo que arma GlobalExceptionHandler: el filtro corre antes que él y el
            // front no debe ver dos formas distintas.
            response.getWriter().write(
                "{\"error\":\"Demasiadas peticiones\",\"message\":\"Has hecho demasiadas peticiones. Espera "
                    + retryAfter + " segundos y vuelve a intentarlo\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private static String pathOf(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return path;
    }

    /** Ventana fija: se abre con la primera petición y cuenta hasta que vence. */
    private static final class Window {
        private final long startedAt;
        private final Quota quota;
        private final AtomicInteger count;

        private Window(long startedAt, Quota quota) {
            this.startedAt = startedAt;
            this.quota = quota;
            this.count = new AtomicInteger(1);
        }
    }

}
