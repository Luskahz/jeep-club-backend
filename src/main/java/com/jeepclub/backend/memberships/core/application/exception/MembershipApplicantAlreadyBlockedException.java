package com.jeepclub.backend.memberships.core.application.exception;

public class MembershipApplicantAlreadyBlockedException extends RuntimeException {

    public MembershipApplicantAlreadyBlockedException(String cpf) {
        super("Já existe um bloqueio ativo para o CPF: " + cpf);
    }
}
