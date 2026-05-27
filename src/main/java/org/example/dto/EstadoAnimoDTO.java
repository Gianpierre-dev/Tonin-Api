package org.example.dto;

import org.example.model.EstadoAnimo;

public record EstadoAnimoDTO(
    Long id,
    String nombre,
    String emoji,
    String iconUrl,
    String musicaUrl,
    String imagenUrl,
    String colorPrimario,
    String colorSecundario,
    String fontFamily,
    String animationType
) {
    public static EstadoAnimoDTO fromEntity(EstadoAnimo estado) {
        return new EstadoAnimoDTO(
            estado.getId(),
            estado.getNombre(),
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
