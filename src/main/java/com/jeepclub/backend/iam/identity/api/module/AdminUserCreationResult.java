package com.jeepclub.backend.iam.identity.api.module;

public record AdminUserCreationResult(Long userId, UserActivationMethod activationMethod,
                                      String temporaryPassword, String invitationStatus) {
}
