package com.jeepclub.backend.authentication.core.domain.exception;

public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException() {
        super("Senha incorreta");
    }
}
