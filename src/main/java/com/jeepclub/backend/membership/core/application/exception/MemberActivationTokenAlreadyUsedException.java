package com.jeepclub.backend.membership.core.application.exception;

public class MemberActivationTokenAlreadyUsedException extends RuntimeException {
    public MemberActivationTokenAlreadyUsedException() {
        super("Este link de ativação já foi utilizado.");
    }
}