package com.jeepclub.backend.memberships.core.application.exception;

public class MemberActivationTokenAlreadyUsedException extends RuntimeException {
    public MemberActivationTokenAlreadyUsedException() {
        super("Este link de ativação já foi utilizado.");
    }
}