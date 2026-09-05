package com.jeepclub.backend.iam.authentication.core.domain.exception.passwordrecovery;

public class PasswordRecoveryRequestStateException
        extends RuntimeException {

    public PasswordRecoveryRequestStateException(
            String message
    ) {
        super(message);
    }
}