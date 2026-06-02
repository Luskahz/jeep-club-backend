package com.jeepclub.backend.memberships.core.application.exception;

public class MembershipApplicationAlreadyProcessedException extends RuntimeException {
    public MembershipApplicationAlreadyProcessedException(Long id, String currentStatus) {
        super("Solicitação " + id + " já foi processada. Status atual: " + currentStatus);
    }
}