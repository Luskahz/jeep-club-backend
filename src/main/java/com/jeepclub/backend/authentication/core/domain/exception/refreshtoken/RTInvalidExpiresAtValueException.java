package com.jeepclub.backend.authentication.core.domain.exception.refreshtoken;

public class RTInvalidExpiresAtValueException extends RuntimeException {
    public RTInvalidExpiresAtValueException(String message) {
        super(message);
    }
}
