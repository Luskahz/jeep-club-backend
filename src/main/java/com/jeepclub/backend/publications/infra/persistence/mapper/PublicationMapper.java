package com.jeepclub.backend.publications.infra.persistence.mapper;

import com.jeepclub.backend.publications.core.domain.model.*;
import com.jeepclub.backend.publications.infra.persistence.entity.*;
import org.springframework.stereotype.Component;

@Component
public class PublicationMapper {
    public Publication toDomain(PublicationEntity entity) {
        var images = entity.getImages().stream()
                .map(image -> new PublicationImage(image.getStorageKey(), image.getPosition(), image.isPrimary()))
                .toList();
        if (entity instanceof NoticeEntity) {
            return Notice.reconstitute(entity.getId(), entity.getAuthorUserId(), entity.getTitle(), entity.getContent(),
                    entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getPublishedAt(), entity.getArchivedAt(), images);
        }
        if (entity instanceof EventEntity event) {
            return Event.reconstitute(entity.getId(), entity.getAuthorUserId(), entity.getTitle(), entity.getContent(),
                    entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getPublishedAt(), entity.getArchivedAt(), images,
                    event.getStartsAt());
        }
        if (entity instanceof ServicePublicationEntity service) {
            return ServicePublication.reconstitute(entity.getId(), entity.getAuthorUserId(), entity.getTitle(), entity.getContent(),
                    entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getPublishedAt(), entity.getArchivedAt(), images,
                    service.getSourceRequestId(), service.getAmount(), service.getContactPhone());
        }
        throw new IllegalArgumentException("Unknown publication entity: " + entity.getClass());
    }

    public PublicationEntity newEntity(Publication domain) {
        PublicationEntity entity;
        if (domain instanceof Notice) entity = new NoticeEntity();
        else if (domain instanceof Event) entity = new EventEntity();
        else if (domain instanceof ServicePublication) entity = new ServicePublicationEntity();
        else throw new IllegalArgumentException("Unknown publication domain: " + domain.getClass());
        copy(domain, entity);
        return entity;
    }

    public void copy(Publication domain, PublicationEntity entity) {
        if (domain instanceof Event event && entity instanceof EventEntity target) target.setStartsAt(event.getStartsAt());
        if (domain instanceof ServicePublication service && entity instanceof ServicePublicationEntity target) {
            target.setSourceRequestId(service.getSourceRequestId());
            target.setAmount(service.getAmount());
            target.setContactPhone(service.getContactPhone());
        }
        if ((domain instanceof Notice && !(entity instanceof NoticeEntity))
                || (domain instanceof Event && !(entity instanceof EventEntity))
                || (domain instanceof ServicePublication && !(entity instanceof ServicePublicationEntity))) {
            throw new IllegalArgumentException("Publication subtype cannot change.");
        }
        entity.setAuthorUserId(domain.getAuthorUserId());
        entity.setTitle(domain.getTitle());
        entity.setContent(domain.getContent());
        entity.setStatus(domain.getStatus());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setPublishedAt(domain.getPublishedAt());
        entity.setArchivedAt(domain.getArchivedAt());
        entity.getImages().clear();
        for (PublicationImage image : domain.getImages()) {
            var mapped = new PublicationImageEntity();
            mapped.setStorageKey(image.storageKey());
            mapped.setPosition(image.position());
            mapped.setPrimary(image.primary());
            entity.getImages().add(mapped);
        }
    }
}
