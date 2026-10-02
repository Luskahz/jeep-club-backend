package com.jeepclub.backend.publications.core.application.service;

import com.jeepclub.backend.publications.core.application.exception.ServiceOperationException;
import com.jeepclub.backend.publications.core.domain.enums.ServicePublicationChangeRequestStatus;
import com.jeepclub.backend.publications.core.domain.model.ServicePublication;
import com.jeepclub.backend.publications.core.domain.model.ServicePublicationChangeRequest;
import com.jeepclub.backend.publications.core.repository.PublicationRepository;
import com.jeepclub.backend.publications.core.repository.ServicePublicationChangeRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import static com.jeepclub.backend.publications.core.application.exception.ServiceOperationException.Reason.*;

@Service
@RequiredArgsConstructor
public class AdminServicePublicationChangeRequestService {
    private final ServicePublicationChangeRequestRepository changes;
    private final PublicationRepository publications;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ServicePublicationChangeRequest findById(Long id) {
        return changes.findById(id).orElseThrow(() -> new ServiceOperationException(CHANGE_REQUEST_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Page<ServicePublicationChangeRequest> findAll(ServicePublicationChangeRequestStatus status, Pageable pageable) {
        return changes.findAll(status, pageable);
    }

    @Transactional
    public ServicePublicationChangeRequest approve(Long id, Long reviewer) {
        Instant now = Instant.now(clock);
        var change = pending(id);
        var publication = publications.findByIdForUpdate(change.getServicePublicationId()).orElse(null);
        if (!(publication instanceof ServicePublication service)) throw new ServiceOperationException(SERVICE_NOT_FOUND);
        if (!service.getAuthorUserId().equals(change.getRequestedByUserId()))
            throw new ServiceOperationException(SERVICE_NOT_OWNER);
        try {
            service.applyApprovedChange(change, now);
        } catch (IllegalStateException exception) {
            throw new ServiceOperationException(SERVICE_INVALID_STATE);
        }
        publications.save(service);
        change.approve(reviewer, now);
        return changes.save(change);
    }

    @Transactional
    public ServicePublicationChangeRequest reject(Long id, Long reviewer, String reason) {
        var change = pending(id);
        change.reject(reviewer, reason, Instant.now(clock));
        return changes.save(change);
    }

    private ServicePublicationChangeRequest pending(Long id) {
        var change = changes.findByIdForUpdate(id)
                .orElseThrow(() -> new ServiceOperationException(CHANGE_REQUEST_NOT_FOUND));
        if (change.getStatus() != ServicePublicationChangeRequestStatus.PENDING)
            throw new ServiceOperationException(CHANGE_REQUEST_ALREADY_PROCESSED);
        return change;
    }
}
