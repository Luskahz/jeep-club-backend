package com.jeepclub.backend.authentication.core.application.exceptions.refreshtoken;

public class RFInvalidException extends RuntimeException {
    public RFInvalidException(String message) {
        super(message);
    }
}
