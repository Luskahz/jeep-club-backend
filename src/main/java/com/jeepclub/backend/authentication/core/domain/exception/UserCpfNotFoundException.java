package com.jeepclub.backend.authentication.core.domain.exception;

public class UserCpfNotFoundException extends RuntimeException {

    public UserCpfNotFoundException() {
        super("Cpf not found");
    }
}
