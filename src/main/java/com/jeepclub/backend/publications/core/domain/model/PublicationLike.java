package com.jeepclub.backend.publications.core.domain.model;

import java.time.Instant;
import java.util.Objects;

public record PublicationLike(Long id, Long publicationId, Long memberUserId, Instant createdAt) {
    public PublicationLike {
        if (id != null) Publication.positive(id, "id");
        Publication.positive(publicationId, "publicationId");
        Publication.positive(memberUserId, "memberUserId");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public static PublicationLike create(Long publicationId, Long memberUserId, Instant now) {
        return new PublicationLike(null, publicationId, memberUserId, now);
    }
}
