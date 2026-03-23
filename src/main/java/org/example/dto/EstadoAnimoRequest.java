package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EstadoAnimoRequest(
    @NotBlank(message = "El nombre del estado no puede estar vacío")
    String nombre,

    @Size(max = 10, message = "El emoji no puede exceder 10 caracteres")
    String emoji,

    @Size(max = 1000, message = "La URL de música no puede exceder 1000 caracteres")
    String musicaUrl,

    @Size(max = 1000, message = "La URL de imagen no puede exceder 1000 caracteres")
    String imagenUrl,

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color primario debe ser un código hex válido (ej: #FF6B6B)")
    String colorPrimario,

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color secundario debe ser un código hex válido (ej: #FFA07A)")
    String colorSecundario,

    @Size(max = 50, message = "El nombre de la fuente no puede exceder 50 caracteres")
    String fontFamily,

    @Size(max = 30, message = "El tipo de animación no puede exceder 30 caracteres")
    String animationType
) {}
