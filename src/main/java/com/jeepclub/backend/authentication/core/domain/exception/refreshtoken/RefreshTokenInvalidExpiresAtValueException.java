package com.jeepclub.backend.authentication.core.domain.exception.refreshtoken;

public class RefreshTokenInvalidExpiresAtValueException extends RuntimeException {
    public RefreshTokenInvalidExpiresAtValueException(String message) {
        super(message);
    }
}
