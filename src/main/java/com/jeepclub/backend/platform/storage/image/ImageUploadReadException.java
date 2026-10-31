package com.jeepclub.backend.platform.storage.image;

/** Failure while materializing request bytes, before any image command is invoked. */
public final class ImageUploadReadException extends RuntimeException {
    public ImageUploadReadException() {
        // Provider exception messages and causes may contain temporary paths or filenames.
        super("Could not read the uploaded image.");
    }
}
