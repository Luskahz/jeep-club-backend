package com.jeepclub.backend.tools.core.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN) // Erro 403
public class ToolAccessDeniedException extends RuntimeException {
    public ToolAccessDeniedException(String message) {
        super(message);
    }
}