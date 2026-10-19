package com.agora.catalog.common.net;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resuelve la IP real del cliente a partir de {@code X-Forwarded-For}.
 *
 * <p><b>Por qué no se toma el primer elemento.</b> El proxy agrega a lo que el cliente ya
 * mandó en vez de sobrescribirlo, así que lo que llega es:
 *
 * <pre>
 *   &lt;lo que inventó el cliente&gt;, &lt;IP real del cliente&gt;
 * </pre>
 *
 * <p>El primer elemento lo controla quien llama: basta mandar {@code X-Forwarded-For: 1.2.3.4}
 * para que esa sea la IP registrada. Con eso, un límite por IP se esquiva con una cabecera y
 * sirve para agotarle el cupo a otro.
 *
 * <p><b>Lo confiable es el final de la lista.</b> Cada proxy propio agrega al final. Contando
 * desde la derecha tantas posiciones como proxies haya delante se llega a la IP real, que el
 * cliente no puede tocar.
 *
 * <p>{@code settings.security.trusted-proxy-count}: <b>0 en local</b> (manda
 * {@code getRemoteAddr()}) y <b>1 detrás de Caddy</b>. Si el número queda corto, se lee una IP
 * interna y el límite pasa a ser global; si queda largo, se cae al valor que mandó el cliente.
 */
@Component
public class ClientIpResolver {

    private final int trustedProxyCount;

    public ClientIpResolver(@Value("${settings.security.trusted-proxy-count:0}") int trustedProxyCount) {
        this.trustedProxyCount = Math.max(0, trustedProxyCount);
    }

    public String resolve(HttpServletRequest request) {
        if (trustedProxyCount == 0) {
            return request.getRemoteAddr();
        }

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) {
            return request.getRemoteAddr();
        }

        String[] hops = forwarded.split(",");
        int index = hops.length - trustedProxyCount;
        if (index < 0 || index >= hops.length) {
            // La cadena es más corta de lo que dice la configuración: alguien se saltó un proxy
            // o el número está mal. Se prefiere la IP de la conexión, que siempre es cierta.
            return request.getRemoteAddr();
        }

        String ip = hops[index].trim();
        return ip.isEmpty() ? request.getRemoteAddr() : ip;
    }

}
