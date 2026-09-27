package com.jeepclub.backend.publications.core.application.exception;

public class ServiceOperationException extends IllegalStateException {
    public enum Reason {
        REQUEST_NOT_FOUND, REQUEST_ALREADY_PROCESSED,
        CHANGE_REQUEST_NOT_FOUND, CHANGE_REQUEST_ALREADY_PROCESSED, CHANGE_REQUEST_ALREADY_PENDING,
        SERVICE_NOT_FOUND, SERVICE_NOT_OWNER, SERVICE_INVALID_STATE, INVALID_REQUEST
    }

    private final Reason reason;

    public ServiceOperationException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}
