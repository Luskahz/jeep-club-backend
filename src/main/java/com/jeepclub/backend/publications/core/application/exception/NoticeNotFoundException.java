package com.jeepclub.backend.publications.core.application.exception;

public class NoticeNotFoundException extends RuntimeException {
    public NoticeNotFoundException(Long id) {
        super("Notice " + id + " was not found.");
    }
}
