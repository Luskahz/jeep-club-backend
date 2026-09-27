package com.jeepclub.backend.publications.infra.persistence.jpa;

import com.jeepclub.backend.publications.infra.persistence.entity.ServicePublicationRequestEntity;
import com.jeepclub.backend.publications.core.domain.enums.ServicePublicationRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ServicePublicationRequestJpaRepository extends JpaRepository<ServicePublicationRequestEntity, Long> {
    @Query(value = "select id from service_publication_requests where id = :id for update", nativeQuery = true)
    Optional<Long> lockId(@Param("id") Long id);
    Page<ServicePublicationRequestEntity> findAllByStatus(ServicePublicationRequestStatus status, Pageable pageable);
}
