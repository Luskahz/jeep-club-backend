package com.jeepclub.backend.publications.core.domain.model;

import com.jeepclub.backend.publications.core.domain.enums.PublicationStatus;
import java.time.Instant;
import java.util.List;

public final class ServicePublication extends Publication {
    private ServicePublication(Long id, Long authorUserId, String title, String content, PublicationStatus status,
                               Instant createdAt, Instant updatedAt, Instant publishedAt, Instant archivedAt,
                               List<PublicationImage> images) {
        super(id, authorUserId, title, content, status, createdAt, updatedAt, publishedAt, archivedAt, images);
    }

    public static ServicePublication create(Long authorUserId, String title, String content,
                                            List<PublicationImage> images, Instant now) {
        return new ServicePublication(null, authorUserId, title, content, PublicationStatus.DRAFT, now, now, null, null, images);
    }

    public static ServicePublication reconstitute(Long id, Long authorUserId, String title, String content,
                                                  PublicationStatus status, Instant createdAt, Instant updatedAt,
                                                  Instant publishedAt, Instant archivedAt, List<PublicationImage> images) {
        positive(id, "id");
        return new ServicePublication(id, authorUserId, title, content, status, createdAt, updatedAt, publishedAt, archivedAt, images);
    }
}
