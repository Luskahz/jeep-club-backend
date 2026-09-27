package com.jeepclub.backend.publications.core.application.service;

import com.jeepclub.backend.publications.core.domain.model.ServicePublication;
import com.jeepclub.backend.publications.core.domain.model.ServicePublicationRequest;
import com.jeepclub.backend.publications.core.repository.PublicationRepository;
import com.jeepclub.backend.publications.core.repository.ServicePublicationRequestRepository;
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
                .orElseThrow(() -> new IllegalArgumentException("Service publication request not found: " + requestId));
        request.requirePending();
        return request;
    }
}
