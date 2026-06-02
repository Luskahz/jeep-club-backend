package com.jeepclub.backend.health.core.application.exceptions;

public class DependentOwnershipValidationUnavailableException extends RuntimeException {

    public DependentOwnershipValidationUnavailableException() {
        super("A validação de vínculo entre dependente e usuário ainda não está disponível.");
    }
}
