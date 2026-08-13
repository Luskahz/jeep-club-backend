package com.jeepclub.backend.memberships.core.application.exception;

public class MembershipEmailAlreadyRegisteredException extends RuntimeException {

    public MembershipEmailAlreadyRegisteredException(String email) {
        super("Já existe um usuário cadastrado com o e-mail: " + email);
    }
}