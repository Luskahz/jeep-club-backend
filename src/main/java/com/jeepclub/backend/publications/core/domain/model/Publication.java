package com.jeepclub.backend.publications.core.domain.model;

import com.jeepclub.backend.publications.core.domain.enums.PublicationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public abstract class Publication {
    private final Long id;
    private final Long authorUserId;
    private final String title;
    private final String content;
    private PublicationStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant publishedAt;
    private Instant archivedAt;
    private List<PublicationImage> images;

    protected Publication(Long id, Long authorUserId, String title, String content,
                          PublicationStatus status, Instant createdAt, Instant updatedAt,
                          Instant publishedAt, Instant archivedAt, List<PublicationImage> images) {
        if (id != null) positive(id, "id");
        positive(authorUserId, "authorUserId");
        this.id = id;
        this.authorUserId = authorUserId;
        this.title = required(title, "title");
        if (this.title.length() > 200) throw new IllegalArgumentException("title exceeds 200 characters.");
        this.content = required(content, "content");
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.publishedAt = publishedAt;
        this.archivedAt = archivedAt;
        if (updatedAt.isBefore(createdAt) || (publishedAt != null && (publishedAt.isBefore(createdAt) || updatedAt.isBefore(publishedAt)))
                || (archivedAt != null && (publishedAt == null || archivedAt.isBefore(publishedAt) || updatedAt.isBefore(archivedAt)))) {
            throw new IllegalArgumentException("Inconsistent publication timestamps.");
        }
        if ((status == PublicationStatus.DRAFT && (publishedAt != null || archivedAt != null))
                || (status == PublicationStatus.PUBLISHED && (publishedAt == null || archivedAt != null))
                || (status == PublicationStatus.ARCHIVED && archivedAt == null)) {
            throw new IllegalArgumentException("Inconsistent editorial status and timestamps.");
        }
        this.images = PublicationGallery.of(images).images();
    }

    public final void publish(Instant now) {
        requireTransitionTime(now);
        if (status != PublicationStatus.DRAFT) throw new IllegalStateException("Only a draft can be published.");
        status = PublicationStatus.PUBLISHED;
        publishedAt = now;
        updatedAt = now;
    }

    public final void archive(Instant now) {
        requireTransitionTime(now);
        if (status != PublicationStatus.PUBLISHED) throw new IllegalStateException("Only a published publication can be archived.");
        status = PublicationStatus.ARCHIVED;
        archivedAt = now;
        updatedAt = now;
    }

    public final void replaceImages(List<PublicationImage> replacement, Instant now) {
        requireTransitionTime(now);
        if (status == PublicationStatus.ARCHIVED) throw new IllegalStateException("Archived publication cannot be edited.");
        List<PublicationImage> validated = PublicationGallery.of(replacement).images();
        images = validated;
        updatedAt = now;
    }

    private void requireTransitionTime(Instant now) {
        Objects.requireNonNull(now, "now");
        if (now.isBefore(updatedAt)) throw new IllegalArgumentException("now cannot precede updatedAt.");
    }

    protected static void positive(Long value, String field) {
        if (value == null || value <= 0) throw new IllegalArgumentException(field + " must be positive.");
    }

    protected static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required.");
        return value.trim();
    }

    public final Long getId() { return id; }
    public final Long getAuthorUserId() { return authorUserId; }
    public final String getTitle() { return title; }
    public final String getContent() { return content; }
    public final PublicationStatus getStatus() { return status; }
    public final Instant getCreatedAt() { return createdAt; }
    public final Instant getUpdatedAt() { return updatedAt; }
    public final Instant getPublishedAt() { return publishedAt; }
    public final Instant getArchivedAt() { return archivedAt; }
    public final List<PublicationImage> getImages() { return images; }
}
