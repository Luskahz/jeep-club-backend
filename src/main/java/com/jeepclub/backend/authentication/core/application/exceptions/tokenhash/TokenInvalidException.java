package com.jeepclub.backend.authentication.core.application.exceptions.tokenhash;

public class TokenInvalidException extends RuntimeException {
    public TokenInvalidException(String message) {
        super(message);
    }
}
