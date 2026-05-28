package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FraseRequest(
    @NotBlank(message = "{validation.frase.texto.notblank}")
    @Size(min = 5, max = 500, message = "{validation.frase.texto.size}")
    String texto,

    @NotNull(message = "{validation.frase.estadoanimoid.notnull}")
    Long estadoAnimoId
) {}
