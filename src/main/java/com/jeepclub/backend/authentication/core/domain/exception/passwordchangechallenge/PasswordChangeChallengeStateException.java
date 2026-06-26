package com.jeepclub.backend.authentication.core.domain.exception.passwordchangechallenge;

public class PasswordChangeChallengeStateException
        extends RuntimeException {

    public PasswordChangeChallengeStateException(
            String message
    ) {
        super(message);
    }
}