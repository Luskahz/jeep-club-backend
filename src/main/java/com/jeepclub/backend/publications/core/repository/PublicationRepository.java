package com.jeepclub.backend.publications.core.repository;

import com.jeepclub.backend.publications.core.domain.model.Publication;
import java.time.Instant;
import java.util.Optional;

public interface PublicationRepository {
    org.springframework.data.domain.Page<com.jeepclub.backend.publications.core.domain.model.Event> findEvents(org.springframework.data.domain.Pageable pageable);
    Publication save(Publication publication);
    Optional<Publication> findById(Long id);
    Optional<Publication> findByIdForUpdate(Long id);
    void delete(Long id, Long deletedByUserId, Instant deletedAt);
}
