package com.jeepclub.backend.publications.core.domain.model;

import com.jeepclub.backend.shared.storage.ImageReference;

public record PublicationCommentImage(String storageKey, int position) {
    public PublicationCommentImage {
        ImageReference.require(storageKey);
        if (position < 0) throw new IllegalArgumentException("Image position must be non-negative.");
    }
}
