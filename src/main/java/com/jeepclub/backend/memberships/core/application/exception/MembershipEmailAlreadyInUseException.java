package com.jeepclub.backend.memberships.core.application.exception;

public class MembershipEmailAlreadyInUseException extends RuntimeException {

    public MembershipEmailAlreadyInUseException(String email) {
        super("Já existe uma solicitação de associação utilizando o e-mail: " + email);
    }
}