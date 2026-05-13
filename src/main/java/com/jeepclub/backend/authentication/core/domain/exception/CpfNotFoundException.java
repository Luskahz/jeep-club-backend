package com.jeepclub.backend.authentication.core.domain.exception;

public class CpfNotFoundException extends RuntimeException {

    public CpfNotFoundException() {
        super("Cpf not found");
    }
}
