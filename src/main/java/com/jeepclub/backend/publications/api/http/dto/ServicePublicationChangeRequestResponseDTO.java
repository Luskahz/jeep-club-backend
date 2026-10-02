package com.jeepclub.backend.publications.api.http.dto;

import com.jeepclub.backend.publications.core.domain.enums.ServicePublicationChangeRequestStatus;
import com.jeepclub.backend.publications.core.domain.model.ServicePublicationChangeRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ServicePublicationChangeRequestResponseDTO(
        Long id, Long servicePublicationId, Long requestedByUserId, String proposedTitle, String proposedContent,
        BigDecimal proposedAmount, String proposedContactPhone, List<PublicationImageResponseDTO> proposedImages,
        ServicePublicationChangeRequestStatus status, String rejectionReason, Instant requestedAt,
        Instant reviewedAt, Instant updatedAt
) {
    public static ServicePublicationChangeRequestResponseDTO from(ServicePublicationChangeRequest request) {
        return new ServicePublicationChangeRequestResponseDTO(request.getId(), request.getServicePublicationId(),
                request.getRequestedByUserId(), request.getProposedTitle(), request.getProposedContent(),
                request.getProposedAmount(), request.getProposedContactPhone(),
                request.getProposedImages().stream().map(PublicationImageResponseDTO::from).toList(),
                request.getStatus(), request.getRejectionReason(), request.getRequestedAt(), request.getReviewedAt(),
                request.getUpdatedAt());
    }
}
