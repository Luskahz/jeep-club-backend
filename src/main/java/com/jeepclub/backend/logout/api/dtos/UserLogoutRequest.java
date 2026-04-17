package com.jeepclub.backend.authentication.api.dtos.logout;

import jakarta.validation.constraints.NotBlank;

public record UserLogoutRequest(
        @NotBlank(message = "Refresh token must not be blank.")
        String refreshToken
) {}
