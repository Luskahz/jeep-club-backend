package com.jeepclub.backend.billing.core.domain.exception.definition;
public class ChargeDefinitionDeletionConflictException extends RuntimeException {
    public ChargeDefinitionDeletionConflictException() { super("Charge definition has retained references."); }
}
