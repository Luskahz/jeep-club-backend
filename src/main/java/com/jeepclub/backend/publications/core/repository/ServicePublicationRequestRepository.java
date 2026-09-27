package com.jeepclub.backend.publications.core.repository;

import com.jeepclub.backend.publications.core.domain.model.ServicePublicationRequest;
import java.util.Optional;

public interface ServicePublicationRequestRepository {
    ServicePublicationRequest save(ServicePublicationRequest request);
    Optional<ServicePublicationRequest> findById(Long id);
    Optional<ServicePublicationRequest> findByIdForUpdate(Long id);
}
