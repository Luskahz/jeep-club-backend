package com.jeepclub.backend.iam.authentication.core.domain.exception.passwordchangechallenge;

public class PasswordChangeChallengeStateException
        extends RuntimeException {

    public PasswordChangeChallengeStateException(
            String message
    ) {
        super(message);
    }
}