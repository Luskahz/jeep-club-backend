package com.jeepclub.backend.publications.core.application.service;

import com.jeepclub.backend.publications.core.domain.exception.PublicationNotFoundException;
import com.jeepclub.backend.publications.core.domain.enums.PublicationStatus;
import com.jeepclub.backend.publications.core.domain.model.PublicationLike;
import com.jeepclub.backend.publications.core.repository.PublicationLikeRepository;
import com.jeepclub.backend.publications.core.repository.PublicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PublicationLikeService {
    private final PublicationRepository publications;
    private final PublicationLikeRepository likes;
    private final Clock clock;

    @Transactional
    public PublicationLike like(Long publicationId, Long memberUserId) {
        requirePublished(publicationId);
        return likes.findByPublicationAndMember(publicationId, memberUserId)
                .orElseGet(() -> likes.save(PublicationLike.create(publicationId, memberUserId, Instant.now(clock))));
    }

    @Transactional
    public void unlike(Long publicationId, Long memberUserId) {
        requirePublished(publicationId);
        likes.findByPublicationAndMember(publicationId, memberUserId).ifPresent(likes::delete);
    }

    private void requirePublished(Long publicationId) {
        var publication = publications.findByIdForUpdate(publicationId)
                .orElseThrow(() -> new PublicationNotFoundException(publicationId));
        if (publication.getStatus() != PublicationStatus.PUBLISHED) {
            throw new PublicationNotFoundException(publicationId);
        }
    }
}
