package com.jeepclub.backend.memberships.core.application.exception;

public class MemberActivationTokenNotFoundException extends RuntimeException {
    public MemberActivationTokenNotFoundException() {
        super("Token de ativação não encontrado ou já utilizado.");
    }
}