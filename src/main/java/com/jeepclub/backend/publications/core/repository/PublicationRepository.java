package com.jeepclub.backend.publications.core.repository;

import com.jeepclub.backend.publications.core.domain.model.Publication;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PublicationRepository {
    Page<Publication> findPublished(String type, Instant from, Instant to, Pageable pageable);
    org.springframework.data.domain.Page<com.jeepclub.backend.publications.core.domain.model.Event> findEvents(org.springframework.data.domain.Pageable pageable);
    Publication save(Publication publication);
    Optional<Publication> findById(Long id);
    Optional<Publication> findByIdForUpdate(Long id);
    void delete(Long id, Long deletedByUserId, Instant deletedAt);
}
