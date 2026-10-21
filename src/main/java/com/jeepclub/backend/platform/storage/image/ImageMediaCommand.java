package com.jeepclub.backend.platform.storage.image;

/** Writes a fresh image inside the caller's database transaction, compensating rollback. */
public interface ImageMediaCommand {
    String storeInTransaction(ImageUpload image);
    /** Same provider and rollback compensation; inaccessible through the global public image resolver. */
    String storePrivateInTransaction(ImageUpload image);
}
