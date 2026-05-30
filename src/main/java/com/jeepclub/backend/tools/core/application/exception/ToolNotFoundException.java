package com.jeepclub.backend.tools.core.application.exception; // <-- Pacote atualizado para application

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ToolNotFoundException extends RuntimeException {
    public ToolNotFoundException(String message) {
        super(message);
    }
}