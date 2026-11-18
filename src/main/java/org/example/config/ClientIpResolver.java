package org.example.config;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resuelve la IP real del cliente detrás del edge de Railway.
 *
 * <p>Se usa {@code X-Real-IP} porque el edge de Railway lo sobrescribe en cada request
 * pública con la IP del cliente, así que el cliente no puede falsificarlo.
 * {@code X-Forwarded-For} no es confiable: su primera entrada la controla el cliente.
 * Además, con {@code server.forward-headers-strategy=framework}, {@code getRemoteAddr()}
 * se deriva de esa misma primera entrada, por lo que solo se usa como respaldo
 * cuando no hay proxy (por ejemplo, en desarrollo local).</p>
 */
public final class ClientIpResolver {

    private static final String REAL_IP_HEADER = "X-Real-IP";

    private ClientIpResolver() {}

    public static String resolver(HttpServletRequest request) {
        String realIp = request.getHeader(REAL_IP_HEADER);
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
