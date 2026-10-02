package com.jeepclub.backend.publications.core.repository;

import com.jeepclub.backend.publications.core.domain.model.PublicationComment;
import java.util.Optional;

public interface PublicationCommentRepository {
    PublicationComment save(PublicationComment comment);
    Optional<PublicationComment> findById(Long id);
    org.springframework.data.domain.Page<PublicationComment> findByPublication(Long publicationId, org.springframework.data.domain.Pageable pageable);
    java.util.Map<Long, Long> counts(java.util.Collection<Long> publicationIds);
}
