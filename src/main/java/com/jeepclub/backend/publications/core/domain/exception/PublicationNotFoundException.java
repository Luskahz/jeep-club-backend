package com.jeepclub.backend.publications.core.domain.exception;

public class PublicationNotFoundException extends RuntimeException {
    public PublicationNotFoundException(Long id) {
        super("Publication " + id + " was not found.");
    }
}
