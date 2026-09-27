package com.jeepclub.backend.publications.core.domain.exception;

public class PublicationAlreadyDeletedException extends RuntimeException {
    public PublicationAlreadyDeletedException(Long id) {
        super("Publication " + id + " no longer exists.");
    }
}
