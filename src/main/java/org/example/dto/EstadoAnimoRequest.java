package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EstadoAnimoRequest(
    @NotBlank(message = "El nombre del estado no puede estar vacío")
    String nombre,

    @Size(max = 10, message = "El emoji no puede exceder 10 caracteres")
    String emoji,

    @Size(max = 1000, message = "La URL de música no puede exceder 1000 caracteres")
    String musicaUrl,

    @Size(max = 1000, message = "La URL de imagen no puede exceder 1000 caracteres")
    String imagenUrl
) {}
