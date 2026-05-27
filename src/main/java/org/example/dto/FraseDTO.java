package org.example.dto;

import org.example.model.Frase;

public record FraseDTO(
    Long id,
    String texto,
    EstadoAnimoDTO estadoAnimo
) {
    public static FraseDTO fromEntity(Frase frase) {
        return new FraseDTO(
            frase.getId(),
            frase.getTexto(),
            EstadoAnimoDTO.fromEntity(frase.getEstadoAnimo())
        );
    }
}
