package com.jeepclub.backend.dependents.core.application.exception;

public class DependentCpfAlreadyInUseException extends RuntimeException {

    public DependentCpfAlreadyInUseException() {
        super("CPF is already in use.");
    }

    public DependentCpfAlreadyInUseException(Throwable cause) {
        super("CPF is already in use.", cause);
    }
}
