package com.jeepclub.backend.dependents.core.application.exception;

public class SocioNotFoundException extends RuntimeException {

    public SocioNotFoundException(Long socioId) {
        super("Socio not found: " + socioId);
    }
}