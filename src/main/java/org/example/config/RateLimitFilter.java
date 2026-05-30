package org.example.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtro de rate limiting para los endpoints de autenticación.
 * Limita a 5 requests por IP cada 60 segundos en /api/auth/login y /api/auth/register.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_MS = 60_000L;
    private static final long CLEANUP_STALE_MS = 5 * 60_000L;
    private static final int CLEANUP_EVERY_N = 100;

    private final MessageSource messageSource;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AtomicInteger invocationCounter = new AtomicInteger(0);

    public RateLimitFilter(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();

        // Solo aplica a los endpoints de autenticación
        if (!path.equals("/api/auth/login") && !path.equals("/api/auth/register")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolverIp(request);

        // Cleanup periódico para evitar crecimiento ilimitado del mapa
        if (invocationCounter.incrementAndGet() % CLEANUP_EVERY_N == 0) {
            limpiarBucketsExpirados();
        }

        Bucket bucket = buckets.computeIfAbsent(clientIp, k -> new Bucket());

        if (bucket.incrementar()) {
            log.warn("Rate limit excedido para IP {}", clientIp);
            String mensaje = messageSource.getMessage(
                    "error.ratelimit.exceeded", null, request.getLocale()
            );
            response.setStatus(429);
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String body = String.format(
                    "{\"timestamp\":%d,\"status\":429,\"message\":\"%s\"}",
                    Instant.now().toEpochMilli(),
                    escaparJson(mensaje)
            );
            response.getWriter().write(body);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolverIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Railway y otros proxies pueden encadenar varias IPs; tomamos la primera
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void limpiarBucketsExpirados() {
        long ahora = System.currentTimeMillis();
        Iterator<Map.Entry<String, Bucket>> it = buckets.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Bucket> entry = it.next();
            if ((ahora - entry.getValue().windowStartMs) > CLEANUP_STALE_MS) {
                it.remove();
            }
        }
    }

    private String escaparJson(String texto) {
        if (texto == null) return "";
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Bucket por IP: contador de requests dentro de la ventana de tiempo activa.
     */
    private static class Bucket {

        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStartMs = System.currentTimeMillis();

        /**
         * Incrementa el contador para la ventana actual.
         * Retorna {@code true} si se superó el límite (debe bloquearse).
         */
        synchronized boolean incrementar() {
            long ahora = System.currentTimeMillis();
            if ((ahora - windowStartMs) >= WINDOW_MS) {
                // La ventana expiró: reiniciar
                windowStartMs = ahora;
                count.set(1);
                return false;
            }
            return count.incrementAndGet() > MAX_REQUESTS;
        }
    }
}
