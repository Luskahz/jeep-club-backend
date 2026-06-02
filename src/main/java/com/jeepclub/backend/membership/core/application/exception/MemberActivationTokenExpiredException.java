package com.jeepclub.backend.membership.core.application.exception;

public class MemberActivationTokenExpiredException extends RuntimeException {
    public MemberActivationTokenExpiredException() {
        super("O link de ativação expirou. Solicite um novo convite ao administrador.");
    }
}