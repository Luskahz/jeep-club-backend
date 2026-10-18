package com.jeepclub.backend.platform.storage.image;

/** Provider- and HTTP-neutral input to the internal image command. */
public record ImageUpload(String filename, String contentType, byte[] content) {
    public static final int MAX_BYTES = 5 * 1024 * 1024;

    public ImageUpload {
        content = content == null ? null : content.clone();
    }

    @Override public byte[] content() {
        return content == null ? null : content.clone();
    }
}
