package com.jeepclub.backend.authentication.core.domain.exception.user;

public class UserCpfNotFoundException extends RuntimeException {

    public UserCpfNotFoundException() {
        super("Cpf not found");
    }
}
