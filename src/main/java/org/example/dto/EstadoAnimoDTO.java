package org.example.dto;

/**
 * Objeto de transferencia de datos para EstadoAnimo.
 * Evita exponer la entidad de base de datos directamente.
 */
public record EstadoAnimoDTO(
    Long id,
    String nombre,
    String emoji,
    String musicaUrl,
    String imagenUrl
) {}
