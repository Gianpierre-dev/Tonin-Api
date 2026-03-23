package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FraseRequest(
    @NotBlank(message = "El texto de la frase no puede estar vacío")
    @Size(min = 5, max = 500, message = "La frase debe tener entre 5 y 500 caracteres")
    String texto,

    @NotNull(message = "Debes asignar un estado de ánimo")
    Long estadoAnimoId
) {}
