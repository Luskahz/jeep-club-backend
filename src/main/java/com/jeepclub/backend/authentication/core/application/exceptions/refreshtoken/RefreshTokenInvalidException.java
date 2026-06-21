package com.jeepclub.backend.authentication.core.application.exceptions.refreshtoken;

public class RefreshTokenInvalidException extends RuntimeException {
    public RefreshTokenInvalidException(String message) {
        super(message);
    }
}
