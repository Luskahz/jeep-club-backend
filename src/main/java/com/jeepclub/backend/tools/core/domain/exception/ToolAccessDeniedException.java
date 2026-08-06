package com.jeepclub.backend.tools.core.domain.exception;

public class ToolAccessDeniedException extends RuntimeException {
    public ToolAccessDeniedException(String message) {
        super(message);
    }
}
