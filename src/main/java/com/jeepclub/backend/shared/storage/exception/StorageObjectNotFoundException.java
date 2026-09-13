package com.jeepclub.backend.shared.storage.exception;

public class StorageObjectNotFoundException extends StorageException {

    public StorageObjectNotFoundException() {
        super("Stored file was not found.");
    }
}
