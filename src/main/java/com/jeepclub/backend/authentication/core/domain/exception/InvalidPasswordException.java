package com.jeepclub.backend.authentication.core.domain.exception;

public class InvalidPasswordException extends RuntimeException {

    public
    InvalidPasswordException(Long userId) {
        super("Invalid password for user id: " + userId);
    }
}
