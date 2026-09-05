package com.jeepclub.backend.iam.authentication.core.domain.exception.passwordchangechallenge;

public class PasswordChangeChallengeValidationException
        extends RuntimeException {

    public PasswordChangeChallengeValidationException(
            String message
    ) {
        super(message);
    }
}