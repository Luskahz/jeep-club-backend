package com.jeepclub.backend.memberships.core.application.exception;

public class MembershipApplicantBlockNotFoundException extends RuntimeException {

    public MembershipApplicantBlockNotFoundException(String cpf) {
        super("Bloqueio ativo não encontrado para o CPF: " + cpf);
    }
}
