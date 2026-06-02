package com.jeepclub.backend.membership.core.application.exception;

public class MemberActivationTokenNotFoundException extends RuntimeException {
    public MemberActivationTokenNotFoundException() {
        super("Token de ativação não encontrado ou já utilizado.");
    }
}