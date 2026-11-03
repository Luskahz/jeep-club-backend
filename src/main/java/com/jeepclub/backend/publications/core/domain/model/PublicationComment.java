package com.jeepclub.backend.publications.core.domain.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public final class PublicationComment {
    public static final int MAX_TEXT_LENGTH = 10_000;
    public static final int MAX_IMAGES = 10;
    private final Long id;
    private final Long publicationId;
    private final Long authorUserId;
    private final String content;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final List<PublicationCommentImage> images;

    private PublicationComment(Long id, Long publicationId, Long authorUserId, String content,
                               Instant createdAt, Instant updatedAt, List<PublicationCommentImage> images) {
        if (id != null) Publication.positive(id, "id");
        Publication.positive(publicationId, "publicationId");
        Publication.positive(authorUserId, "authorUserId");
        this.id = id;
        this.publicationId = publicationId;
        this.authorUserId = authorUserId;
        this.content = content == null ? "" : content.trim();
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        if (updatedAt.isBefore(createdAt)) throw new IllegalArgumentException("updatedAt cannot precede createdAt.");
        this.images = validateImages(images);
        if (this.content.isEmpty() && this.images.isEmpty()) {
            throw new IllegalArgumentException("Comment needs text or at least one image.");
        }
    }

    public static PublicationComment create(Long publicationId, Long authorUserId, String content,
                                            List<PublicationCommentImage> images, Instant now) {
        validatePayloadSize(content, images == null ? 0 : images.size());
        return new PublicationComment(null, publicationId, authorUserId, content, now, now, images);
    }

    /** Input-only caps: historical comments remain readable after limits are introduced. */
    public static void validatePayloadSize(String content, int imageCount) {
        if (content != null && content.length() > MAX_TEXT_LENGTH)
            throw new IllegalArgumentException("Comment text exceeds " + MAX_TEXT_LENGTH + " characters.");
        if (imageCount > MAX_IMAGES)
            throw new IllegalArgumentException("Comment exceeds " + MAX_IMAGES + " images.");
    }

    public static PublicationComment reconstitute(Long id, Long publicationId, Long authorUserId, String content,
                                                  Instant createdAt, Instant updatedAt, List<PublicationCommentImage> images) {
        Publication.positive(id, "id");
        return new PublicationComment(id, publicationId, authorUserId, content, createdAt, updatedAt, images);
    }

    private static List<PublicationCommentImage> validateImages(List<PublicationCommentImage> images) {
        if (images == null) throw new IllegalArgumentException("images is required.");
        var positions = new HashSet<Integer>();
        var keys = new HashSet<String>();
        for (PublicationCommentImage image : images) {
            if (!positions.add(image.position()) || !keys.add(image.storageKey())) {
                throw new IllegalArgumentException("Comment image positions and keys must be unique.");
            }
        }
        for (int position = 0; position < images.size(); position++) {
            if (!positions.contains(position)) throw new IllegalArgumentException("Comment image positions must be contiguous from zero.");
        }
        return List.copyOf(images.stream().sorted(java.util.Comparator.comparingInt(PublicationCommentImage::position)).toList());
    }

    public Long getId() { return id; }
    public Long getPublicationId() { return publicationId; }
    public Long getAuthorUserId() { return authorUserId; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<PublicationCommentImage> getImages() { return images; }
}
