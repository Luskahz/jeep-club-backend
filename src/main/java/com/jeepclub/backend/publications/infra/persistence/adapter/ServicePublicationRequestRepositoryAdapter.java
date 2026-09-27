package com.jeepclub.backend.publications.infra.persistence.adapter;

import com.jeepclub.backend.publications.core.domain.model.ServicePublicationRequest;
import com.jeepclub.backend.publications.core.domain.enums.ServicePublicationRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.jeepclub.backend.publications.core.repository.ServicePublicationRequestRepository;
import com.jeepclub.backend.publications.infra.persistence.jpa.ServicePublicationRequestJpaRepository;
import com.jeepclub.backend.publications.infra.persistence.mapper.ServicePublicationRequestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ServicePublicationRequestRepositoryAdapter implements ServicePublicationRequestRepository {
    private final ServicePublicationRequestJpaRepository requests;
    private final ServicePublicationRequestMapper mapper;

    @Override
    public ServicePublicationRequest save(ServicePublicationRequest request) {
        return mapper.toDomain(requests.saveAndFlush(mapper.toEntity(request)));
    }

    @Override
    public Optional<ServicePublicationRequest> findById(Long id) {
        return requests.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ServicePublicationRequest> findByIdForUpdate(Long id) {
        return requests.lockId(id).flatMap(requests::findById).map(mapper::toDomain);
    }

    @Override
    public Page<ServicePublicationRequest> findAll(ServicePublicationRequestStatus status, Pageable pageable) {
        return (status == null ? requests.findAll(pageable) : requests.findAllByStatus(status, pageable)).map(mapper::toDomain);
    }
}
