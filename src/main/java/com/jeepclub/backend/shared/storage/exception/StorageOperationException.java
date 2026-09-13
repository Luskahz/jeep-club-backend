package com.jeepclub.backend.shared.storage.exception;

import java.util.Objects;

public class StorageOperationException extends StorageException {

    private final Operation operation;

    public StorageOperationException(Operation operation, Throwable cause) {
        super(messageFor(operation), cause);
        this.operation = operation;
    }

    public Operation operation() {
        return operation;
    }

    private static String messageFor(Operation operation) {
        return switch (Objects.requireNonNull(operation, "operation cannot be null")) {
            case READ -> "Could not read stored file.";
            case WRITE -> "Could not store file.";
            case DELETE -> "Could not delete stored file.";
        };
    }

    public enum Operation {
        READ,
        WRITE,
        DELETE
    }
}
