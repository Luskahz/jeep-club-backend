package com.jeepclub.backend.shared.storage;

public record StorageFile(
        String originalFilename,
        String contentType,
        String extension,
        byte[] content
) {

    public StorageFile {
        content = content == null ? null : content.clone();
    }

    @Override
    public byte[] content() {
        return content == null ? null : content.clone();
    }
}
