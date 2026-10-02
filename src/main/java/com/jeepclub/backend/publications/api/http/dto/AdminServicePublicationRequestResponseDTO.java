package com.jeepclub.backend.publications.api.http.dto;

import com.jeepclub.backend.publications.core.domain.enums.ServicePublicationRequestStatus;
import com.jeepclub.backend.publications.core.domain.model.ServicePublicationRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AdminServicePublicationRequestResponseDTO(
        Long id, Long requestedByUserId, String title, String content, BigDecimal amount, String contactPhone,
        List<PublicationImageResponseDTO> images, ServicePublicationRequestStatus status, String rejectionReason,
        Long reviewedByUserId, Long createdPublicationId, Instant requestedAt, Instant reviewedAt, Instant updatedAt,
        Long version
) {
    public static AdminServicePublicationRequestResponseDTO from(ServicePublicationRequest request) {
        return new AdminServicePublicationRequestResponseDTO(request.getId(), request.getRequestedByUserId(),
                request.getTitle(), request.getContent(), request.getAmount(), request.getContactPhone(),
                request.getImages().stream().map(PublicationImageResponseDTO::from).toList(), request.getStatus(),
                request.getRejectionReason(), request.getReviewedByUserId(), request.getCreatedPublicationId(),
                request.getRequestedAt(), request.getReviewedAt(), request.getUpdatedAt(), request.getVersion());
    }
}
