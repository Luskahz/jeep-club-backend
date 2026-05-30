package com.jeepclub.backend.tools.core.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) // Spring já converte para erro 409
public class InvalidToolStatusException extends RuntimeException {
    public InvalidToolStatusException(String message) {
        super(message);
    }
}