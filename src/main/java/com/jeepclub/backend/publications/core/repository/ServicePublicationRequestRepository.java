package com.jeepclub.backend.publications.core.repository;

import com.jeepclub.backend.publications.core.domain.model.ServicePublicationRequest;
import com.jeepclub.backend.publications.core.domain.enums.ServicePublicationRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface ServicePublicationRequestRepository {
    ServicePublicationRequest save(ServicePublicationRequest request);
    Optional<ServicePublicationRequest> findById(Long id);
    Optional<ServicePublicationRequest> findByIdForUpdate(Long id);
    Page<ServicePublicationRequest> findAll(ServicePublicationRequestStatus status, Pageable pageable);
}
