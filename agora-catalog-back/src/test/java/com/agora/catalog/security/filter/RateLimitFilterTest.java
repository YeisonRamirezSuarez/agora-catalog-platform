package com.agora.catalog.security.filter;

import com.agora.catalog.common.net.ClientIpResolver;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private final RateLimitFilter filter = new RateLimitFilter(new ClientIpResolver(0));

    private int call(String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api" + path);
        request.setContextPath("/api");
        request.setRemoteAddr("190.25.1.8");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }

    @Test
    void cadaRutaTieneSuCupo() {
        assertThat(RateLimitFilter.quotaFor("/auth/login")).isEqualTo(RateLimitFilter.Quota.AUTH);
        assertThat(RateLimitFilter.quotaFor("/auth/refresh")).isEqualTo(RateLimitFilter.Quota.AUTH);
        assertThat(RateLimitFilter.quotaFor("/public/catalogos/x7k2m9ab")).isEqualTo(RateLimitFilter.Quota.PUBLIC);
        assertThat(RateLimitFilter.quotaFor("/admin/productos")).isNull();
    }

    @Test
    void elLoginSeCortaAlPasarElCupoSinGastarElDelCatalogo() throws Exception {
        for (int i = 0; i < RateLimitFilter.Quota.AUTH.maxRequests; i++) {
            assertThat(call("/auth/login")).isEqualTo(200);
        }
        assertThat(call("/auth/login")).isEqualTo(429);
        assertThat(call("/public/catalogos/x7k2m9ab")).isEqualTo(200);
    }
}
