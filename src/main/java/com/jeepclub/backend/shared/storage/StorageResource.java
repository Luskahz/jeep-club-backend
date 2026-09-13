package com.jeepclub.backend.shared.storage;

public record StorageResource(
        String storageKey,
        byte[] content
) {

    public StorageResource {
        content = content == null ? null : content.clone();
    }

    @Override
    public byte[] content() {
        return content == null ? null : content.clone();
    }

    public long size() {
        return content == null ? 0 : content.length;
    }
}
