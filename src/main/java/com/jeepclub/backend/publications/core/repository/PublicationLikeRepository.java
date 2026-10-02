package com.jeepclub.backend.publications.core.repository;

import com.jeepclub.backend.publications.core.domain.model.PublicationLike;

public interface PublicationLikeRepository {
    PublicationLike save(PublicationLike like);
    java.util.Optional<PublicationLike> findByPublicationAndMember(Long publicationId, Long memberUserId);
    void delete(PublicationLike like);
    java.util.Map<Long, Long> counts(java.util.Collection<Long> publicationIds);
    java.util.Set<Long> likedByMember(java.util.Collection<Long> publicationIds, Long memberUserId);
}
