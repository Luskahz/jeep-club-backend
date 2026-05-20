package com.jeepclub.backend.authentication.core.application.results;

import com.jeepclub.backend.authentication.core.domain.model.PasswordRecoveryRequest;



public record PasswordResetLinkAdminResult(
        String token,
        String resetLink,
        PasswordRecoveryRequest recoveryRequest
) {
}