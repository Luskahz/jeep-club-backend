package com.jeepclub.backend.authentication.api.dto.refresh;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequestDTO(
        @NotBlank
        String refreshToken
) {}