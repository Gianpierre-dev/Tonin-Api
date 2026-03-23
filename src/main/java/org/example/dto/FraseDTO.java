package org.example.dto;

/**
 * Objeto de transferencia de datos para Frase.
 * Incluye la información del estado de ánimo asociado.
 */
public record FraseDTO(
    Long id,
    String texto,
    EstadoAnimoDTO estadoAnimo
) {}
