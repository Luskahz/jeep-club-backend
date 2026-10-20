package com.jeepclub.backend.platform.storage.image;

/** Internal read capability. The consuming context MUST authorize ownership/permissions first. */
public interface PrivateImageMediaQuery {
    ImageMediaContent loadPrivate(String key);
}
