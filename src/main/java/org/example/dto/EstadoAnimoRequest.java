package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record EstadoAnimoRequest(
    @NotBlank(message = "{validation.estado.codigo.notblank}")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "{validation.estado.codigo.pattern}")
    String codigo,

    @Size(max = 10, message = "{validation.estado.emoji.size}")
    String emoji,

    @Size(max = 1000, message = "{validation.estado.iconurl.size}")
    String iconUrl,

    @Size(max = 1000, message = "{validation.estado.musicaurl.size}")
    String musicaUrl,

    @Size(max = 1000, message = "{validation.estado.imagenurl.size}")
    String imagenUrl,

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "{validation.estado.colorprimario.pattern}")
    String colorPrimario,

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "{validation.estado.colorsecundario.pattern}")
    String colorSecundario,

    @Size(max = 50, message = "{validation.estado.fontfamily.size}")
    String fontFamily,

    @Size(max = 30, message = "{validation.estado.animationtype.size}")
    String animationType,

    @NotEmpty(message = "{validation.estado.traducciones.notempty}")
    @Size(max = 10, message = "{validation.traducciones.size}")
    Map<String, String> traducciones
) {}
