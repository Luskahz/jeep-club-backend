package com.jeepclub.backend.tools.core.application.exception;

public class ToolNotFoundException extends RuntimeException {
    public ToolNotFoundException(String message) {
        super(message);
    }
}
