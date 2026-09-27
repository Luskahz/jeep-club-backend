package com.jeepclub.backend.publications.core.application.exception;

public class InvalidNoticeStateException extends RuntimeException {
    public InvalidNoticeStateException(String message) {
        super(message);
    }
}
