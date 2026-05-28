package org.example.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record FraseRequest(
    @NotEmpty(message = "{validation.frase.traducciones.notempty}")
    Map<String, String> traducciones,

    @NotNull(message = "{validation.frase.estadoanimoid.notnull}")
    Long estadoAnimoId
) {}
