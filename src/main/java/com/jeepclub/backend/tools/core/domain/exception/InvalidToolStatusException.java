package com.jeepclub.backend.tools.core.domain.exception;

public class InvalidToolStatusException extends RuntimeException {
    public InvalidToolStatusException(String message) {
        super(message);
    }
}
