package org.example.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "{validation.username.notblank}")
    String username,

    @NotBlank(message = "{validation.password.notblank}")
    String password
) {}
