package com.jeepclub.backend.billing.api.module;

public final class EventBillingException extends RuntimeException {
    public enum Reason { CHARGE_NOT_FOUND, INVALID_CONFIGURATION }
    private final Reason reason;

    public EventBillingException(String message) { this(Reason.INVALID_CONFIGURATION, message); }
    public EventBillingException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }
    public Reason reason() { return reason; }
}
