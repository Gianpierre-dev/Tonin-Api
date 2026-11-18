package org.example.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.setUseCodeAsDefaultMessage(true);
        filter = new RateLimitFilter(messageSource);
    }

    private MockHttpServletResponse enviarLogin(String realIp, String forwardedFor, String remoteAddr) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setServletPath("/api/auth/login");
        request.setRemoteAddr(remoteAddr);
        if (realIp != null) request.addHeader("X-Real-IP", realIp);
        if (forwardedFor != null) request.addHeader("X-Forwarded-For", forwardedFor);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void bloqueaAlSextoIntentoDesdeLaMismaIp() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertThat(enviarLogin("203.0.113.10", null, "10.0.0.1").getStatus()).isEqualTo(200);
        }
        MockHttpServletResponse bloqueada = enviarLogin("203.0.113.10", null, "10.0.0.1");
        assertThat(bloqueada.getStatus()).isEqualTo(429);
        assertThat(bloqueada.getContentAsString()).contains("\"status\":429");
    }

    @Test
    void noSePuedeEvadirFalsificandoXForwardedFor() throws Exception {
        for (int i = 0; i < 5; i++) {
            enviarLogin("203.0.113.10", "198.51.100." + i, "10.0.0.1");
        }
        assertThat(enviarLogin("203.0.113.10", "198.51.100.99", "10.0.0.1").getStatus()).isEqualTo(429);
    }

    @Test
    void sinXRealIpUsaLaDireccionRemota() throws Exception {
        for (int i = 0; i < 5; i++) {
            enviarLogin(null, null, "192.0.2.7");
        }
        assertThat(enviarLogin(null, null, "192.0.2.7").getStatus()).isEqualTo(429);
        assertThat(enviarLogin(null, null, "192.0.2.8").getStatus()).isEqualTo(200);
    }

    @Test
    void noAplicaAEndpointsFueraDeAuth() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/estados");
            request.setServletPath("/api/estados");
            request.addHeader("X-Real-IP", "203.0.113.10");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }
}
