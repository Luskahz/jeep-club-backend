package com.jeepclub.backend.dependents.core.application.exception;


public class DependentCpfAlreadyInUseException extends RuntimeException {

    public DependentCpfAlreadyInUseException(String cpf) {
        super("CPF is already in use: " + cpf);
    }
}
