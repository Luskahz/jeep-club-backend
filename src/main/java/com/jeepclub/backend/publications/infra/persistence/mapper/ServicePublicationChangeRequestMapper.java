package com.jeepclub.backend.publications.infra.persistence.mapper;

import com.jeepclub.backend.publications.core.domain.enums.ServicePublicationChangeRequestStatus;
import com.jeepclub.backend.publications.core.domain.model.PublicationImage;
import com.jeepclub.backend.publications.core.domain.model.ServicePublicationChangeRequest;
import com.jeepclub.backend.publications.infra.persistence.entity.PublicationImageEntity;
import com.jeepclub.backend.publications.infra.persistence.entity.ServicePublicationChangeRequestEntity;
import org.springframework.stereotype.Component;

@Component
public class ServicePublicationChangeRequestMapper {
    public ServicePublicationChangeRequest toDomain(ServicePublicationChangeRequestEntity entity) {
        var images = entity.getProposedImages().stream()
                .map(image -> new PublicationImage(image.getStorageKey(), image.getPosition(), image.isPrimary()))
                .toList();
        return ServicePublicationChangeRequest.reconstitute(entity.getId(), entity.getServicePublicationId(),
                entity.getRequestedByUserId(), entity.getProposedTitle(), entity.getProposedContent(),
                entity.getProposedAmount(), entity.getProposedContactPhone(), images, entity.getStatus(),
                entity.getRejectionReason(), entity.getReviewedByUserId(), entity.getRequestedAt(),
                entity.getReviewedAt(), entity.getUpdatedAt(), entity.getVersion());
    }

    public ServicePublicationChangeRequestEntity toEntity(ServicePublicationChangeRequest request) {
        var entity = new ServicePublicationChangeRequestEntity();
        entity.setId(request.getId());
        entity.setVersion(request.getVersion());
        entity.setServicePublicationId(request.getServicePublicationId());
        entity.setPendingServicePublicationId(request.getStatus() == ServicePublicationChangeRequestStatus.PENDING
                ? request.getServicePublicationId() : null);
        entity.setRequestedByUserId(request.getRequestedByUserId());
        entity.setProposedTitle(request.getProposedTitle());
        entity.setProposedContent(request.getProposedContent());
        entity.setProposedAmount(request.getProposedAmount());
        entity.setProposedContactPhone(request.getProposedContactPhone());
        entity.setStatus(request.getStatus());
        entity.setRejectionReason(request.getRejectionReason());
        entity.setReviewedByUserId(request.getReviewedByUserId());
        entity.setRequestedAt(request.getRequestedAt());
        entity.setReviewedAt(request.getReviewedAt());
        entity.setUpdatedAt(request.getUpdatedAt());
        for (PublicationImage image : request.getProposedImages()) {
            var mapped = new PublicationImageEntity();
            mapped.setStorageKey(image.storageKey());
            mapped.setPosition(image.position());
            mapped.setPrimary(image.primary());
            entity.getProposedImages().add(mapped);
        }
        return entity;
    }
}
