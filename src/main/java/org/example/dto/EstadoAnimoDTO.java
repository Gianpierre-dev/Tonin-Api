package org.example.dto;

import org.example.model.EstadoAnimo;
import org.example.model.EstadoAnimoTraduccion;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Shape unificado: además del 'nombre' resuelto por Accept-Language,
 * expone el mapa completo de traducciones (locale -> nombre) para que
 * el CRUD admin pueda poblar el formulario sin endpoints alternativos.
 */
public record EstadoAnimoDTO(
    Long id,
    String codigo,
    String nombre,
    Map<String, String> traducciones,
    String emoji,
    String iconUrl,
    String musicaUrl,
    String imagenUrl,
    String colorPrimario,
    String colorSecundario,
    String fontFamily,
    String animationType
) {
    public static EstadoAnimoDTO fromEntity(EstadoAnimo estado, String locale) {
        Map<String, String> traducciones = estado.getTraducciones().stream()
            .collect(Collectors.toMap(
                EstadoAnimoTraduccion::getLocale,
                EstadoAnimoTraduccion::getNombre,
                (a, b) -> a,
                LinkedHashMap::new
            ));
        return new EstadoAnimoDTO(
            estado.getId(),
            estado.getCodigo(),
            estado.resolverNombre(locale),
            traducciones,
            estado.getEmoji(),
            estado.getIconUrl(),
            estado.getMusicaUrl(),
            estado.getImagenUrl(),
            estado.getColorPrimario(),
            estado.getColorSecundario(),
            estado.getFontFamily(),
            estado.getAnimationType()
        );
    }
}
