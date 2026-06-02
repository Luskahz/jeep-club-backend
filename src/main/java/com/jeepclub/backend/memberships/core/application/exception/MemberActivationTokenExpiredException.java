package com.jeepclub.backend.memberships.core.application.exception;

public class MemberActivationTokenExpiredException extends RuntimeException {
    public MemberActivationTokenExpiredException() {
        super("O link de ativação expirou. Solicite um novo convite ao administrador.");
    }
}