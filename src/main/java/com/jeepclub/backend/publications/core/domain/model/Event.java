package com.jeepclub.backend.publications.core.domain.model;

import com.jeepclub.backend.publications.core.domain.enums.PublicationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class Event extends Publication {
    private final Instant startsAt;

    private Event(Long id, Long authorUserId, String title, String content, PublicationStatus status,
                  Instant createdAt, Instant updatedAt, Instant publishedAt, Instant archivedAt,
                  List<PublicationImage> images, Instant startsAt) {
        super(id, authorUserId, title, content, status, createdAt, updatedAt, publishedAt, archivedAt, images);
        this.startsAt = Objects.requireNonNull(startsAt, "startsAt");
    }

    public static Event create(Long authorUserId, String title, String content, List<PublicationImage> images,
                               Instant startsAt, Instant now) {
        return new Event(null, authorUserId, title, content, PublicationStatus.DRAFT, now, now, null, null, images, startsAt);
    }

    public static Event reconstitute(Long id, Long authorUserId, String title, String content, PublicationStatus status,
                                    Instant createdAt, Instant updatedAt, Instant publishedAt, Instant archivedAt,
                                    List<PublicationImage> images, Instant startsAt) {
        positive(id, "id");
        return new Event(id, authorUserId, title, content, status, createdAt, updatedAt, publishedAt, archivedAt, images, startsAt);
    }

    public Instant getStartsAt() { return startsAt; }
}
