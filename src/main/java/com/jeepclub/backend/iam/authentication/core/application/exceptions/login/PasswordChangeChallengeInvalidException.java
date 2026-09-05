package com.jeepclub.backend.iam.authentication.core.application.exceptions.login;

public class PasswordChangeChallengeInvalidException extends RuntimeException {
    public PasswordChangeChallengeInvalidException(String message) {
        super(message);
    }
}
