package org.example.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Hallazgo 13: el token identifica al usuario por su id, no solo por el username.
 */
class JwtServiceTest {

    private static final String SECRET = "clave-de-prueba-de-al-menos-32-bytes-para-hs256!!";

    private final JwtService jwtService = new JwtService(SECRET, 60_000);

    private static UsuarioPrincipal principal(long id, String username) {
        return new UsuarioPrincipal(id, username, "encoded", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void tokenValidoParaElMismoUsuario() {
        String token = jwtService.generateToken(principal(1L, "ana"));

        assertTrue(jwtService.isTokenValid(token, principal(1L, "ana")));
    }

    @Test
    void tokenRechazadoSiElUsuarioFueRecreadoConElMismoUsername() {
        String token = jwtService.generateToken(principal(1L, "ana"));

        assertFalse(jwtService.isTokenValid(token, principal(2L, "ana")));
    }

    @Test
    void tokenSinIdDeUsuario_SeRechaza() {
        // Formato anterior: solo subject. Tras el deploy hay que volver a iniciar sesión.
        String viejo = Jwts.builder()
                .subject("ana")
                .issuer("tonin-api")
                .audience().add("tonin-web").and()
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertFalse(jwtService.isTokenValid(viejo, principal(1L, "ana")));
    }

    @Test
    void elTokenNoExponeLosRoles() {
        String token = jwtService.generateToken(principal(1L, "ana"));
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);

        assertFalse(payload.contains("roles"), payload);
        assertFalse(payload.contains("ROLE_ADMIN"), payload);
    }
}
