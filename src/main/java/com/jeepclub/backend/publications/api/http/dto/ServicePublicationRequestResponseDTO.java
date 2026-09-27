package com.jeepclub.backend.publications.api.http.dto;

import com.jeepclub.backend.publications.core.domain.enums.ServicePublicationRequestStatus;
import com.jeepclub.backend.publications.core.domain.model.ServicePublicationRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Solicitação inicial de serviço; dados do revisor só aparecem na superfície administrativa.")
public record ServicePublicationRequestResponseDTO(
        Long id, Long requestedByUserId, String title, String content, BigDecimal amount, String contactPhone,
        List<PublicationImageResponseDTO> images, ServicePublicationRequestStatus status, String rejectionReason,
        Long createdPublicationId, Instant requestedAt, Instant reviewedAt, Instant updatedAt
) {
    public static ServicePublicationRequestResponseDTO from(ServicePublicationRequest request) {
        return new ServicePublicationRequestResponseDTO(request.getId(), request.getRequestedByUserId(),
                request.getTitle(), request.getContent(), request.getAmount(), request.getContactPhone(),
                request.getImages().stream().map(PublicationImageResponseDTO::from).toList(), request.getStatus(),
                request.getRejectionReason(), request.getCreatedPublicationId(), request.getRequestedAt(),
                request.getReviewedAt(), request.getUpdatedAt());
    }
}
