package com.jeepclub.backend.authentication.api.dto.recovery;

public record PasswordRecoveryAdminResponseDTO(
        String temporaryPassword,
        String resetToken
) {
}
