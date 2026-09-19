package com.jeepclub.backend.health.core.application.exceptions;

public class MedicalProfilePersistenceException extends RuntimeException {

    public MedicalProfilePersistenceException(Throwable cause) {
        super("Não foi possível concluir a operação do perfil médico.", cause);
    }
}
