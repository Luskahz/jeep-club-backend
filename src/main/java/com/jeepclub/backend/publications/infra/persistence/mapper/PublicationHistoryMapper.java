package com.jeepclub.backend.publications.infra.persistence.mapper;

import com.jeepclub.backend.publications.infra.persistence.entity.*;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
public class PublicationHistoryMapper {
    public PublicationHistoryEntity snapshot(PublicationEntity source, Long deletedByUserId, Instant deletedAt) {
        PublicationHistoryEntity history;
        if (source instanceof NoticeEntity) history = new NoticeHistoryEntity();
        else if (source instanceof EventEntity event) {
            var eventHistory = new EventHistoryEntity();
            eventHistory.setStartsAt(event.getStartsAt());
            history = eventHistory;
        } else if (source instanceof ServicePublicationEntity service) {
            var serviceHistory = new ServicePublicationHistoryEntity();
            serviceHistory.setSourceRequestId(service.getSourceRequestId());
            serviceHistory.setAmount(service.getAmount());
            serviceHistory.setContactPhone(service.getContactPhone());
            history = serviceHistory;
        }
        else throw new IllegalArgumentException("Unknown publication entity: " + source.getClass());

        history.setPublicationId(source.getId());
        history.setAuthorUserId(source.getAuthorUserId());
        history.setTitle(source.getTitle());
        history.setContent(source.getContent());
        history.setStatus(source.getStatus());
        history.setCreatedAt(source.getCreatedAt());
        history.setUpdatedAt(source.getUpdatedAt());
        history.setPublishedAt(source.getPublishedAt());
        history.setArchivedAt(source.getArchivedAt());
        history.setDeletedByUserId(deletedByUserId);
        history.setDeletedAt(deletedAt);
        for (PublicationImageEntity image : source.getImages()) {
            var copy = new PublicationImageEntity();
            copy.setStorageKey(image.getStorageKey());
            copy.setPosition(image.getPosition());
            copy.setPrimary(image.isPrimary());
            history.getImages().add(copy);
        }
        return history;
    }
}
