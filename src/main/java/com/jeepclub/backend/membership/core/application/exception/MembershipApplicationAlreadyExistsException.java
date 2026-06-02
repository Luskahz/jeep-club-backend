package com.jeepclub.backend.membership.core.application.exception;

public class MembershipApplicationAlreadyExistsException extends RuntimeException {
    public MembershipApplicationAlreadyExistsException(String cpf) {
        super("Já existe uma solicitação para o CPF: " + cpf);
    }
}