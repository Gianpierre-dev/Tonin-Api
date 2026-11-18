package org.example.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService implements IJwtService {

    private static final String ISSUER = "tonin-api";
    private static final String AUDIENCE = "tonin-web";
    private static final String CLAIM_USER_ID = "uid";

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:1800000}") long expirationMs) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "La variable de entorno JWT_SECRET es obligatoria y no puede estar vacia");
        }
        // UTF-8 explicito: el charset por defecto del JVM varia entre plataformas.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * El token lleva el username (subject) y el id del usuario (claim "uid"). Los roles NO van en
     * el token: se recargan de la BD en cada request, así un cambio de rol aplica de inmediato.
     */
    public String generateToken(UserDetails userDetails) {
        if (!(userDetails instanceof UsuarioPrincipal principal)) {
            throw new IllegalArgumentException("Se esperaba un UsuarioPrincipal para emitir el token");
        }

        return Jwts.builder()
                .claim(CLAIM_USER_ID, principal.getId())
                .subject(userDetails.getUsername())
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * Válido si el username y el id coinciden con el usuario actual de la BD y no expiró. Comparar
     * el id evita que un token de un usuario borrado sirva para otro creado con el mismo username.
     * Los tokens sin "uid" (formato anterior) se rechazan.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final Claims claims = extractAllClaims(token);
        final Long userId = claims.get(CLAIM_USER_ID, Long.class);
        return userDetails instanceof UsuarioPrincipal principal
                && claims.getSubject().equals(principal.getUsername())
                && userId != null
                && userId.equals(principal.getId())
                && !claims.getExpiration().before(new Date());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        // Parser estricto: requiere issuer y audience esperados.
        // Tokens emitidos por otros servicios (aunque compartan secret) son rechazados.
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .requireAudience(AUDIENCE)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
