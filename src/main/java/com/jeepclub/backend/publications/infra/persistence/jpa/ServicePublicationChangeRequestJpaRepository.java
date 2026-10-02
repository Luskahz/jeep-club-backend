package com.jeepclub.backend.publications.infra.persistence.jpa;

import com.jeepclub.backend.publications.core.domain.enums.ServicePublicationChangeRequestStatus;
import com.jeepclub.backend.publications.infra.persistence.entity.ServicePublicationChangeRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ServicePublicationChangeRequestJpaRepository extends JpaRepository<ServicePublicationChangeRequestEntity, Long> {
    @Query(value = "select id from service_publication_change_requests where id = :id for update", nativeQuery = true)
    Optional<Long> lockId(@Param("id") Long id);
    boolean existsByPendingServicePublicationId(Long servicePublicationId);
    Page<ServicePublicationChangeRequestEntity> findAllByStatus(ServicePublicationChangeRequestStatus status, Pageable pageable);
}
