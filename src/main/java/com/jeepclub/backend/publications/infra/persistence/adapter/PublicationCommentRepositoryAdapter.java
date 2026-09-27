package com.jeepclub.backend.publications.infra.persistence.adapter;

import com.jeepclub.backend.publications.core.domain.model.PublicationComment;
import com.jeepclub.backend.publications.core.domain.model.PublicationCommentImage;
import com.jeepclub.backend.publications.core.repository.PublicationCommentRepository;
import com.jeepclub.backend.publications.infra.persistence.entity.PublicationCommentEntity;
import com.jeepclub.backend.publications.infra.persistence.entity.PublicationCommentImageEntity;
import com.jeepclub.backend.publications.infra.persistence.jpa.PublicationCommentJpaRepository;
import com.jeepclub.backend.publications.infra.persistence.jpa.PublicationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PublicationCommentRepositoryAdapter implements PublicationCommentRepository {
    private final PublicationCommentJpaRepository comments;
    private final PublicationJpaRepository publications;

    @Override
    public PublicationComment save(PublicationComment comment) {
        var entity = new PublicationCommentEntity();
        entity.setId(comment.getId());
        entity.setPublication(publications.getReferenceById(comment.getPublicationId()));
        entity.setAuthorUserId(comment.getAuthorUserId());
        entity.setContent(comment.getContent());
        entity.setCreatedAt(comment.getCreatedAt());
        entity.setUpdatedAt(comment.getUpdatedAt());
        for (PublicationCommentImage image : comment.getImages()) {
            var mapped = new PublicationCommentImageEntity();
            mapped.setStorageKey(image.storageKey());
            mapped.setPosition(image.position());
            entity.getImages().add(mapped);
        }
        var saved = comments.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<PublicationComment> findById(Long id) {
        return comments.findById(id).map(this::toDomain);
    }

    @Override
    public org.springframework.data.domain.Page<PublicationComment> findByPublication(Long publicationId, org.springframework.data.domain.Pageable pageable) {
        return comments.findByPublication_Id(publicationId, pageable).map(this::toDomain);
    }

    @Override
    public java.util.Map<Long, Long> counts(java.util.Collection<Long> publicationIds) {
        if (publicationIds.isEmpty()) return java.util.Map.of();
        var result = new java.util.HashMap<Long, Long>();
        comments.countByPublicationIds(publicationIds).forEach(row -> result.put((Long) row[0], (Long) row[1]));
        return java.util.Map.copyOf(result);
    }

    private PublicationComment toDomain(PublicationCommentEntity entity) {
        var images = entity.getImages().stream()
                .map(image -> new PublicationCommentImage(image.getStorageKey(), image.getPosition()))
                .toList();
        return PublicationComment.reconstitute(entity.getId(), entity.getPublication().getId(), entity.getAuthorUserId(),
                entity.getContent(), entity.getCreatedAt(), entity.getUpdatedAt(), images);
    }
}
