package com.jeepclub.backend.memberships.core.application.exception;

public class MembershipApplicationNotFoundException extends RuntimeException {
    public MembershipApplicationNotFoundException(Long id) {
        super("Solicitação de membro não encontrada: " + id);
    }
}