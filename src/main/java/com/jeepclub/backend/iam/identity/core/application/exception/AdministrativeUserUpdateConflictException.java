package com.jeepclub.backend.iam.identity.core.application.exception;

public class AdministrativeUserUpdateConflictException extends RuntimeException {
    public AdministrativeUserUpdateConflictException() { super("Administrative user update conflicts with uniqueness or protected identity policy."); }
    public AdministrativeUserUpdateConflictException(Throwable cause) { super("CPF, email or RG is already registered.", cause); }
}
