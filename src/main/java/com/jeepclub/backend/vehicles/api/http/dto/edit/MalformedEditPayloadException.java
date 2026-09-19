package com.jeepclub.backend.vehicles.api.http.dto.edit;

public class MalformedEditPayloadException extends RuntimeException {
    public MalformedEditPayloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
