package com.jeepclub.backend.tools.core.application.exception;

public final class InvalidToolSortException extends RuntimeException {
    public InvalidToolSortException(String field) { super("Invalid tool sort field: " + field); }
}
