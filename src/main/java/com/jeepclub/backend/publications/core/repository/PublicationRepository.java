package com.jeepclub.backend.publications.core.repository;

import com.jeepclub.backend.publications.core.domain.model.Publication;
import java.time.Instant;
import java.util.Optional;

public interface PublicationRepository {
    Publication save(Publication publication);
    Optional<Publication> findById(Long id);
    Optional<Publication> findByIdForUpdate(Long id);
    void delete(Long id, Long deletedByUserId, Instant deletedAt);
}
