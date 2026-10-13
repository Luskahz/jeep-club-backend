package com.jeepclub.backend.iam.authentication.core.application.exceptions.account;

public class ActivationInvitationException extends RuntimeException {
    private final boolean conflict;

    public ActivationInvitationException(boolean conflict) {
        super(conflict ? "Account is not eligible for activation." : "Invitation is invalid or expired.");
        this.conflict = conflict;
    }

    public boolean isConflict() {
        return conflict;
    }
}
