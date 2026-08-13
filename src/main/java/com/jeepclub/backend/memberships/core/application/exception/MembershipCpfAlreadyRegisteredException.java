package com.jeepclub.backend.memberships.core.application.exception;

public class MembershipCpfAlreadyRegisteredException extends RuntimeException {

    public MembershipCpfAlreadyRegisteredException(String cpf) {
        super("Já existe um usuário cadastrado com o CPF: " + cpf);
    }
}
