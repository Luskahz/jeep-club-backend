package com.jeepclub.backend.authentication.api.dtos.refresh;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequestDTO(
        @NotBlank
        String refreshToken
) {}