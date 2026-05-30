package org.example.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record FraseRequest(
    @NotEmpty(message = "{validation.frase.traducciones.notempty}")
    @Size(max = 10, message = "{validation.traducciones.size}")
    Map<String, String> traducciones,

    @NotNull(message = "{validation.frase.estadoanimoid.notnull}")
    Long estadoAnimoId
) {}
