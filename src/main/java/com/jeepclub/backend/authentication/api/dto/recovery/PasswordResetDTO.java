package com.jeepclub.backend.authentication.api.dto.recovery;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetDTO(
        @NotBlank String token,
        @NotBlank String newPassword
) {
}
