package com.jeepclub.backend.publications.infra.persistence.mapper;

import com.jeepclub.backend.publications.core.domain.model.PublicationImage;
import com.jeepclub.backend.publications.core.domain.model.ServicePublicationRequest;
import com.jeepclub.backend.publications.infra.persistence.entity.PublicationImageEntity;
import com.jeepclub.backend.publications.infra.persistence.entity.ServicePublicationRequestEntity;
import org.springframework.stereotype.Component;

@Component
public class ServicePublicationRequestMapper {
    public ServicePublicationRequest toDomain(ServicePublicationRequestEntity entity) {
        var images = entity.getImages().stream()
                .map(image -> new PublicationImage(image.getStorageKey(), image.getPosition(), image.isPrimary()))
                .toList();
        return ServicePublicationRequest.reconstitute(entity.getId(), entity.getRequestedByUserId(), entity.getTitle(),
                entity.getContent(), entity.getAmount(), entity.getContactPhone(), images, entity.getStatus(),
                entity.getRejectionReason(), entity.getReviewedByUserId(), entity.getCreatedPublicationId(),
                entity.getRequestedAt(), entity.getReviewedAt(), entity.getUpdatedAt(), entity.getVersion());
    }

    public ServicePublicationRequestEntity toEntity(ServicePublicationRequest request) {
        var entity = new ServicePublicationRequestEntity();
        entity.setId(request.getId());
        entity.setVersion(request.getVersion());
        entity.setRequestedByUserId(request.getRequestedByUserId());
        entity.setTitle(request.getTitle());
        entity.setContent(request.getContent());
        entity.setAmount(request.getAmount());
        entity.setContactPhone(request.getContactPhone());
        entity.setStatus(request.getStatus());
        entity.setRejectionReason(request.getRejectionReason());
        entity.setReviewedByUserId(request.getReviewedByUserId());
        entity.setCreatedPublicationId(request.getCreatedPublicationId());
        entity.setRequestedAt(request.getRequestedAt());
        entity.setReviewedAt(request.getReviewedAt());
        entity.setUpdatedAt(request.getUpdatedAt());
        for (PublicationImage image : request.getImages()) {
            var mapped = new PublicationImageEntity();
            mapped.setStorageKey(image.storageKey());
            mapped.setPosition(image.position());
            mapped.setPrimary(image.primary());
            entity.getImages().add(mapped);
        }
        return entity;
    }
}
