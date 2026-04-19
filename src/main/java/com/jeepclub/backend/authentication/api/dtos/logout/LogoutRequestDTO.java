package com.jeepclub.backend.authentication.api.dtos.logout;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequestDTO(
        @NotBlank String refreshToken
) {}