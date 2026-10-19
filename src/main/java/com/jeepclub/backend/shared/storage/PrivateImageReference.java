package com.jeepclub.backend.shared.storage;

import com.jeepclub.backend.shared.storage.exception.InvalidStorageKeyException;

/** Private images use a disjoint namespace that the global public resolver rejects. */
public final class PrivateImageReference {
    private PrivateImageReference() {}

    public static String require(String key) {
        if (key == null || !key.matches("private-images/\\d{4}/\\d{2}/\\d{2}/[0-9a-fA-F-]{36}\\.(png|jpg|webp)")) {
            throw new InvalidStorageKeyException("A valid private image reference is required.");
        }
        return key;
    }
}
