package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "{validation.username.notblank}")
    @Size(min = 3, max = 50, message = "{validation.username.size}")
    String username,

    @NotBlank(message = "{validation.password.notblank}")
    // max 72: límite de BCrypt. AuthService además valida los 72 bytes en UTF-8.
    @Size(min = 6, max = 72, message = "{validation.password.size}")
    String password
) {}
