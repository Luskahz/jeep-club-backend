package com.jeepclub.backend.shared.storage;

import com.jeepclub.backend.shared.storage.exception.InvalidStorageKeyException;

/** Stable, provider-neutral reference to an image uploaded through the global media API. */
public final class ImageReference {
    private static final String PREFIX = "images/";

    private ImageReference() {}

    public static String require(String key) {
        if (key == null || !key.startsWith(PREFIX)
                || !key.matches("images/\\d{4}/\\d{2}/\\d{2}/[0-9a-fA-F-]{36}\\.(png|jpg|webp)")) {
            throw new InvalidStorageKeyException("A valid image storage key is required.");
        }
        return key;
    }
}
