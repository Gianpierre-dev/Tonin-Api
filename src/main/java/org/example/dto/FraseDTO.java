package org.example.dto;

import org.example.model.Frase;
import org.example.model.FraseTraduccion;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Shape unificado: además del 'texto' resuelto por Accept-Language,
 * expone el mapa completo de traducciones (locale -> texto) para que
 * el CRUD admin pueda poblar el formulario sin endpoints alternativos.
 */
public record FraseDTO(
    Long id,
    String texto,
    Map<String, String> traducciones,
    EstadoAnimoDTO estadoAnimo
) {
    public static FraseDTO fromEntity(Frase frase, String locale) {
        Map<String, String> traducciones = frase.getTraducciones().stream()
            .collect(Collectors.toMap(
                FraseTraduccion::getLocale,
                FraseTraduccion::getTexto,
                (a, b) -> a,
                LinkedHashMap::new
            ));
        return new FraseDTO(
            frase.getId(),
            frase.resolverTexto(locale),
            traducciones,
            EstadoAnimoDTO.fromEntity(frase.getEstadoAnimo(), locale)
        );
    }
}
