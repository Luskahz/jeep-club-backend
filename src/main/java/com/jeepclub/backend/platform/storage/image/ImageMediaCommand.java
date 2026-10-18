package com.jeepclub.backend.platform.storage.image;

/** Writes a fresh image inside the caller's database transaction, compensating rollback. */
public interface ImageMediaCommand {
    String storeInTransaction(ImageUpload image);
}
