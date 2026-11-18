package org.example.service;

import org.example.exception.BadRequestException;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reglas compartidas para los mapas de traducciones (locale -> texto) que llegan en los requests.
 */
final class Traducciones {

    static final String LOCALE_DEFAULT = "es";

    /** Locales que la app sirve (ver I18nConfig). Cualquier otro se guardaría y nunca se mostraría. */
    static final Set<String> LOCALES_SOPORTADOS = Set.of("es", "en");

    /** Largo máximo del texto de una frase (columna frase_traduccion.texto). */
    static final int MAX_TEXTO_FRASE = 500;

    /** Largo máximo del nombre de un estado (columna estado_animo_traduccion.nombre). */
    static final int MAX_NOMBRE_ESTADO = 255;

    private Traducciones() {}

    /**
     * Normaliza y valida un mapa de traducciones:
     * <ul>
     *   <li>descarta las entradas con valor nulo o en blanco (equivalen a "sin traducción" y no deben
     *       anular el fallback a "es");</li>
     *   <li>normaliza el locale a minúsculas y solo acepta los {@link #LOCALES_SOPORTADOS};</li>
     *   <li>rechaza el mismo locale repetido con distintas mayúsculas ("es" y "ES");</li>
     *   <li>exige la traducción en español y respeta el largo máximo de la columna.</li>
     * </ul>
     *
     * @return un mapa nuevo solo con las traducciones con contenido, en el orden recibido.
     */
    static Map<String, String> normalizarYValidar(Map<String, String> traducciones, int maxLength) {
        Map<String, String> normalizadas = new LinkedHashMap<>();
        if (traducciones != null) {
            traducciones.forEach((locale, texto) -> {
                if (texto == null || texto.isBlank()) return;
                String clave = locale == null ? "" : locale.trim().toLowerCase(Locale.ROOT);
                if (!LOCALES_SOPORTADOS.contains(clave)) {
                    throw new BadRequestException("error.traducciones.locale.invalid", locale, LOCALES_SOPORTADOS);
                }
                if (normalizadas.putIfAbsent(clave, texto) != null) {
                    throw new BadRequestException("error.traducciones.locale.duplicated", clave);
                }
            });
        }
        if (!normalizadas.containsKey(LOCALE_DEFAULT)) {
            throw new BadRequestException("error.traducciones.esrequired");
        }
        for (String texto : normalizadas.values()) {
            if (texto.length() > maxLength) {
                throw new BadRequestException("error.traducciones.texto.size", maxLength);
            }
        }
        return normalizadas;
    }
}
