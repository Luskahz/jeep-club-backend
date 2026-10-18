package com.jeepclub.backend.memberships.core.domain.exception;

/** Sanitized errors: never include input, SQL or personal data in the message. */
public class MemberRegistryException extends RuntimeException {
    public enum Kind { INVALID, NOT_FOUND, CONFLICT, DISABLED }
    private final Kind kind;
    public MemberRegistryException(Kind kind) {
        super(switch (kind) {
            case INVALID -> "Invalid member registry input.";
            case NOT_FOUND -> "Member registry resource not found.";
            case CONFLICT -> "Member registry operation conflicts with current state.";
            case DISABLED -> "Automatic membership number issuance is disabled.";
        });
        this.kind = kind;
    }
    public Kind getKind() { return kind; }
}
