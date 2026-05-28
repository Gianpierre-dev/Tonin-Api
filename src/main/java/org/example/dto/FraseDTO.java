package org.example.dto;

import org.example.model.Frase;

public record FraseDTO(
    Long id,
    String texto,
    EstadoAnimoDTO estadoAnimo
) {
    public static FraseDTO fromEntity(Frase frase, String locale) {
        return new FraseDTO(
            frase.getId(),
            frase.resolverTexto(locale),
            EstadoAnimoDTO.fromEntity(frase.getEstadoAnimo(), locale)
        );
    }
}
