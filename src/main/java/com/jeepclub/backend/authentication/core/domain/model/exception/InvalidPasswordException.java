package com.jeepclub.backend.authentication.core.domain.model.exception;

public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException() {
        super("Senha incorreta");
    }
}
