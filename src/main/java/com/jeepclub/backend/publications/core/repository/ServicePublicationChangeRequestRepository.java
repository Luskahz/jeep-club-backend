package com.jeepclub.backend.publications.core.repository;

import com.jeepclub.backend.publications.core.domain.enums.ServicePublicationChangeRequestStatus;
import com.jeepclub.backend.publications.core.domain.model.ServicePublicationChangeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface ServicePublicationChangeRequestRepository {
    ServicePublicationChangeRequest save(ServicePublicationChangeRequest request);
    Optional<ServicePublicationChangeRequest> findById(Long id);
    Optional<ServicePublicationChangeRequest> findByIdForUpdate(Long id);
    boolean existsPendingForService(Long servicePublicationId);
    Page<ServicePublicationChangeRequest> findAll(ServicePublicationChangeRequestStatus status, Pageable pageable);
}
