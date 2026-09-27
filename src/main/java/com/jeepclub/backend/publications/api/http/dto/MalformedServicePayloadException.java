package com.jeepclub.backend.publications.api.http.dto;

public class MalformedServicePayloadException extends RuntimeException {
    public MalformedServicePayloadException(String message) { super(message); }
    public MalformedServicePayloadException(String message, Throwable cause) { super(message, cause); }
}
