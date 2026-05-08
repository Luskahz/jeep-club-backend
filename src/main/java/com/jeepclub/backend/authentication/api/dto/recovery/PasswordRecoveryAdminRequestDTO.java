package com.jeepclub.backend.authentication.api.dto.recovery;

import jakarta.validation.constraints.NotNull;

public record PasswordRecoveryAdminRequestDTO(
        @NotNull Long targetUserId,
        @NotNull Boolean generateTempPassword
) {
}
