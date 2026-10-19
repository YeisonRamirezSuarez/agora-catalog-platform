package com.agora.catalog.common.net;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private static MockHttpServletRequest request(String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        if (forwardedFor != null) request.addHeader("X-Forwarded-For", forwardedFor);
        return request;
    }

    @Test
    void sinProxiesUsaLaConexionAunqueLleguenCabeceras() {
        assertThat(new ClientIpResolver(0).resolve(request("1.2.3.4"))).isEqualTo("10.0.0.1");
    }

    @Test
    void conUnProxyIgnoraLoQueInventoElCliente() {
        assertThat(new ClientIpResolver(1).resolve(request("1.2.3.4, 190.25.1.8"))).isEqualTo("190.25.1.8");
    }

    @Test
    void cadenaMasCortaQueLosProxiesCaeALaConexion() {
        assertThat(new ClientIpResolver(2).resolve(request("190.25.1.8"))).isEqualTo("10.0.0.1");
        assertThat(new ClientIpResolver(1).resolve(request(null))).isEqualTo("10.0.0.1");
    }
}
