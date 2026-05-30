package com.jeepclub.backend.medical.core.application.exceptions;

public class MedicalProfileNotFoundException extends RuntimeException {

    public MedicalProfileNotFoundException() {
        super("Perfil médico não encontrado.");
    }
}
