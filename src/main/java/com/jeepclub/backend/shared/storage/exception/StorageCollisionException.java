package com.jeepclub.backend.shared.storage.exception;

public class StorageCollisionException extends StorageException {

    public StorageCollisionException() {
        super("A stored file already exists for the generated key.");
    }
}
