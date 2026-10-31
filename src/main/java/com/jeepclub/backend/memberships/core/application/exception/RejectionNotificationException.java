package com.jeepclub.backend.memberships.core.application.exception;

import lombok.Getter;

@Getter
public class RejectionNotificationException extends RuntimeException {
    public enum Kind { NOT_FOUND, NOT_REJECTED, PROCESSING, NO_RECIPIENT, PROVIDER_FAILED, RESULT_UNKNOWN }
    private final Kind kind;
    public RejectionNotificationException(Kind kind) {
        super("Rejection notification operation: " + kind.name());
        this.kind = kind;
    }
}
