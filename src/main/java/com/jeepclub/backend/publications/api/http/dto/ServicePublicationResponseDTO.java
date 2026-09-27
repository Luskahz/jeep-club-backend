package com.jeepclub.backend.publications.api.http.dto;

import com.jeepclub.backend.publications.core.domain.enums.PublicationStatus;
import com.jeepclub.backend.publications.core.domain.model.ServicePublication;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ServicePublicationResponseDTO(
        Long id, Long authorUserId, Long sourceRequestId, String title, String content, BigDecimal amount,
        String contactPhone, PublicationStatus status, List<PublicationImageResponseDTO> images,
        Instant createdAt, Instant updatedAt, Instant publishedAt, Instant archivedAt
) {
    public static ServicePublicationResponseDTO from(ServicePublication service) {
        return new ServicePublicationResponseDTO(service.getId(), service.getAuthorUserId(), service.getSourceRequestId(),
                service.getTitle(), service.getContent(), service.getAmount(), service.getContactPhone(),
                service.getStatus(), service.getImages().stream().map(PublicationImageResponseDTO::from).toList(),
                service.getCreatedAt(), service.getUpdatedAt(), service.getPublishedAt(), service.getArchivedAt());
    }
}
