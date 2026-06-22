package com.jeepclub.backend.authentication.core.domain.enums;

public enum UserStatus {
    /**
     * Legacy API projection. Business rules use AccountStatus,
     * AuthenticationStatus and CredentialStatus instead.
     */
    ACTIVE,
    LOCKED,
    DISABLED,
    PENDING_FIRST_ACCESS,
    CHANGE_PASSWORD_REQUIRED
}
