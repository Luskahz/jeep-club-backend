package com.jeepclub.backend.shared.storage.exception;

public abstract class StorageException extends RuntimeException {

    protected StorageException(String message) {
        super(message);
    }

    protected StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
