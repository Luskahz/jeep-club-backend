package com.jeepclub.backend.publications.infra.persistence.adapter;

import com.jeepclub.backend.publications.core.domain.enums.ServicePublicationChangeRequestStatus;
import com.jeepclub.backend.publications.core.domain.model.ServicePublicationChangeRequest;
import com.jeepclub.backend.publications.core.repository.ServicePublicationChangeRequestRepository;
import com.jeepclub.backend.publications.infra.persistence.jpa.ServicePublicationChangeRequestJpaRepository;
import com.jeepclub.backend.publications.infra.persistence.mapper.ServicePublicationChangeRequestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ServicePublicationChangeRequestRepositoryAdapter implements ServicePublicationChangeRequestRepository {
    private final ServicePublicationChangeRequestJpaRepository requests;
    private final ServicePublicationChangeRequestMapper mapper;

    @Override public ServicePublicationChangeRequest save(ServicePublicationChangeRequest request) {
        return mapper.toDomain(requests.saveAndFlush(mapper.toEntity(request)));
    }
    @Override public Optional<ServicePublicationChangeRequest> findById(Long id) {
        return requests.findById(id).map(mapper::toDomain);
    }
    @Override public Optional<ServicePublicationChangeRequest> findByIdForUpdate(Long id) {
        return requests.lockId(id).flatMap(requests::findById).map(mapper::toDomain);
    }
    @Override public boolean existsPendingForService(Long servicePublicationId) {
        return requests.existsByPendingServicePublicationId(servicePublicationId);
    }
    @Override public Page<ServicePublicationChangeRequest> findAll(ServicePublicationChangeRequestStatus status, Pageable pageable) {
        return (status == null ? requests.findAll(pageable) : requests.findAllByStatus(status, pageable)).map(mapper::toDomain);
    }
}
