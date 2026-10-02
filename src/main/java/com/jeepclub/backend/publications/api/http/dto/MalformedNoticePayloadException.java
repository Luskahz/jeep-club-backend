package com.jeepclub.backend.publications.api.http.dto;

public class MalformedNoticePayloadException extends RuntimeException {
    public MalformedNoticePayloadException(String message) { super(message); }
    public MalformedNoticePayloadException(String message, Throwable cause) { super(message, cause); }
}
