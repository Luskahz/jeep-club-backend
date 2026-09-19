package com.jeepclub.backend.shared.storage;

public interface FileStorage {

    StoredFile store(StorageFile file, String namespace);

    StorageResource load(String storageKey);

    void delete(String storageKey);
}
