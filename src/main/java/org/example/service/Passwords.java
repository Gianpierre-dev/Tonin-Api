package org.example.service;

import java.nio.charset.StandardCharsets;

/**
 * Reglas de contraseña compartidas por el registro y el bootstrap del admin.
 */
final class Passwords {

    /** BCrypt solo usa los primeros 72 bytes: lo que sigue se ignoraría (o falla, según la versión). */
    static final int MAX_BYTES = 72;

    static final int MIN_CARACTERES = 6;

    private Passwords() {}

    static boolean excedeLimiteBcrypt(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES;
    }
}
