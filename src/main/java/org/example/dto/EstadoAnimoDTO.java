package org.example.dto;

public record EstadoAnimoDTO(
    Long id,
    String nombre,
    String emoji,
    String musicaUrl,
    String imagenUrl,
    String colorPrimario,
    String colorSecundario,
    String fontFamily,
    String animationType
) {}
