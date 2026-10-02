package com.jeepclub.backend.publications.core.domain.model;

import com.jeepclub.backend.publications.core.domain.enums.PublicationStatus;
import java.time.Instant;
import java.util.List;

public final class Notice extends Publication {
    private Notice(Long id, Long authorUserId, String title, String content, PublicationStatus status,
                   Instant createdAt, Instant updatedAt, Instant publishedAt, Instant archivedAt,
                   List<PublicationImage> images) {
        super(id, authorUserId, title, content, status, createdAt, updatedAt, publishedAt, archivedAt, images);
    }

    public static Notice create(Long authorUserId, String title, String content, List<PublicationImage> images, Instant now) {
        return new Notice(null, authorUserId, title, content, PublicationStatus.DRAFT, now, now, null, null, images);
    }

    public static Notice reconstitute(Long id, Long authorUserId, String title, String content, PublicationStatus status,
                                     Instant createdAt, Instant updatedAt, Instant publishedAt, Instant archivedAt,
                                     List<PublicationImage> images) {
        positive(id, "id");
        return new Notice(id, authorUserId, title, content, status, createdAt, updatedAt, publishedAt, archivedAt, images);
    }
}
