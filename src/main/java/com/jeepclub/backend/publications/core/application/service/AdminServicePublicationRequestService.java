package com.jeepclub.backend.publications.core.application.service;

import com.jeepclub.backend.publications.core.domain.model.ServicePublication;
import com.jeepclub.backend.publications.core.domain.model.ServicePublicationRequest;
import com.jeepclub.backend.publications.core.repository.PublicationRepository;
import com.jeepclub.backend.publications.core.repository.ServicePublicationRequestRepository;
import com.jeepclub.backend.publications.core.application.exception.ServiceOperationException;
import com.jeepclub.backend.publications.core.domain.enums.ServicePublicationRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import static com.jeepclub.backend.publications.core.application.exception.ServiceOperationException.Reason.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AdminServicePublicationRequestService {
    private final ServicePublicationRequestRepository requests;
    private final PublicationRepository publications;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ServicePublicationRequest findById(Long id) {
        return requests.findById(id).orElseThrow(() -> new ServiceOperationException(REQUEST_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Page<ServicePublicationRequest> findAll(ServicePublicationRequestStatus status, Pageable pageable) {
        return requests.findAll(status, pageable);
    }

    @Transactional
    public ServicePublicationRequest approve(Long requestId, Long reviewedByUserId) {
        Instant now = Instant.now(clock);
        var request = pending(requestId);
        if (reviewedByUserId == null || reviewedByUserId <= 0) throw new IllegalArgumentException("reviewedByUserId must be positive.");
        var service = ServicePublication.fromApprovedRequest(request, now);
        var saved = publications.save(service);
        request.approve(reviewedByUserId, saved.getId(), now);
        return requests.save(request);
    }

    @Transactional
    public ServicePublicationRequest reject(Long requestId, Long reviewedByUserId, String reason) {
        Instant now = Instant.now(clock);
        var request = pending(requestId);
        request.reject(reviewedByUserId, reason, now);
        return requests.save(request);
    }

    private ServicePublicationRequest pending(Long requestId) {
        var request = requests.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ServiceOperationException(REQUEST_NOT_FOUND));
        if (request.getStatus() != ServicePublicationRequestStatus.PENDING)
            throw new ServiceOperationException(REQUEST_ALREADY_PROCESSED);
        return request;
    }
}
