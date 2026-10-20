package com.jeepclub.backend.platform.storage.image;

/** Read result without storage keys, provider paths or original filenames. */
public record ImageMediaContent(String contentType, byte[] content) {
    public ImageMediaContent { content = content.clone(); }
    @Override public byte[] content() { return content.clone(); }
    public int size() { return content.length; }
}
