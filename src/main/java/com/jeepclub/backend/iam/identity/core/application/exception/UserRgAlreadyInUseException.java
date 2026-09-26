package com.jeepclub.backend.iam.identity.core.application.exception;

public class UserRgAlreadyInUseException extends RuntimeException {
    public UserRgAlreadyInUseException() { super("RG is already registered."); }
    public UserRgAlreadyInUseException(Throwable cause) { super("RG is already registered.", cause); }
}
