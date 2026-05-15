package com.jeepclub.backend.authentication.api.dto.recovery;

import jakarta.validation.constraints.NotBlank;

public record PasswordRecoveryRequestDTO(
        @NotBlank String cpf
) {
}
