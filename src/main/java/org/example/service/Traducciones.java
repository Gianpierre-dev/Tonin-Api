package org.example.service;

import org.example.exception.BadRequestException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reglas compartidas para los mapas de traducciones (locale -> texto) que llegan en los requests.
 */
final class Traducciones {

    static final String LOCALE_DEFAULT = "es";
    private static final int MAX_LENGTH = 500;

    private Traducciones() {}

    /**
     * Descarta las entradas con valor nulo o en blanco (equivalen a "sin traducción" y no deben
     * anular el fallback a "es"), valida que exista la traducción en español y el largo máximo.
     *
     * @return un mapa nuevo solo con las traducciones con contenido, en el orden recibido.
     */
    static Map<String, String> normalizarYValidar(Map<String, String> traducciones) {
        Map<String, String> normalizadas = new LinkedHashMap<>();
        if (traducciones != null) {
            traducciones.forEach((locale, texto) -> {
                if (texto != null && !texto.isBlank()) {
                    normalizadas.put(locale, texto);
                }
            });
        }
        if (!normalizadas.containsKey(LOCALE_DEFAULT)) {
            throw new BadRequestException("error.traducciones.esrequired");
        }
        for (String texto : normalizadas.values()) {
            if (texto.length() > MAX_LENGTH) {
                throw new BadRequestException("error.traducciones.texto.size");
            }
        }
        return normalizadas;
    }
}
