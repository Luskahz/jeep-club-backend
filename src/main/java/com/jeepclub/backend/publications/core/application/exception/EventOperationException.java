package com.jeepclub.backend.publications.core.application.exception;

public class EventOperationException extends RuntimeException {
    private final String code;
    public EventOperationException(String code) { super(code.replace('_', ' ').toLowerCase()); this.code = code; }
    public String getCode() { return code; }
}
