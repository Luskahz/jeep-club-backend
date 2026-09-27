package com.jeepclub.backend.publications.infra.persistence.adapter;

import com.jeepclub.backend.publications.core.domain.exception.PublicationAlreadyDeletedException;
import com.jeepclub.backend.publications.core.domain.model.Publication;
import com.jeepclub.backend.publications.core.repository.PublicationRepository;
import com.jeepclub.backend.publications.infra.persistence.entity.PublicationEntity;
import com.jeepclub.backend.publications.infra.persistence.jpa.*;
import com.jeepclub.backend.publications.infra.persistence.mapper.PublicationHistoryMapper;
import com.jeepclub.backend.publications.infra.persistence.mapper.PublicationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import com.jeepclub.backend.publications.core.domain.enums.PublicationStatus;
import com.jeepclub.backend.publications.infra.persistence.entity.NoticeEntity;
import com.jeepclub.backend.publications.infra.persistence.entity.EventEntity;
import com.jeepclub.backend.publications.infra.persistence.entity.ServicePublicationEntity;

@Repository
@RequiredArgsConstructor
public class PublicationRepositoryAdapter implements PublicationRepository {
    @Override
    public Page<Publication> findPublished(String type, Instant from, Instant to, Pageable pageable) {
        Specification<PublicationEntity> filter = (root, query, cb) -> {
            var conditions = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            conditions.add(cb.equal(root.get("status"), PublicationStatus.PUBLISHED));
            if (from != null) conditions.add(cb.greaterThanOrEqualTo(root.get("publishedAt"), from));
            if (to != null) conditions.add(cb.lessThanOrEqualTo(root.get("publishedAt"), to));
            if (type != null) {
                Class<? extends PublicationEntity> subtype = switch (type) {
                    case "NOTICE" -> NoticeEntity.class;
                    case "EVENT" -> EventEntity.class;
                    case "SERVICE" -> ServicePublicationEntity.class;
                    default -> throw new IllegalArgumentException("Unsupported publication type.");
                };
                conditions.add(cb.equal(root.type(), subtype));
            }
            return cb.and(conditions.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return publications.findAll(filter, pageable).map(mapper::toDomain);
    }
    private final PublicationJpaRepository publications;
    private final PublicationHistoryJpaRepository history;
    private final PublicationLikeJpaRepository likes;
    private final PublicationCommentJpaRepository comments;
    private final PublicationMapper mapper;
    private final PublicationHistoryMapper historyMapper;
    @Override
    public org.springframework.data.domain.Page<com.jeepclub.backend.publications.core.domain.model.Event> findEvents(org.springframework.data.domain.Pageable pageable) {
        return publications.findEvents(pageable).map(e -> (com.jeepclub.backend.publications.core.domain.model.Event) mapper.toDomain(e));
    }

    @Override
    public Publication save(Publication publication) {
        PublicationEntity entity;
        if (publication.getId() == null) entity = mapper.newEntity(publication);
        else {
            entity = lockedEntity(publication.getId())
                    .orElseThrow(() -> new PublicationAlreadyDeletedException(publication.getId()));
            mapper.copy(publication, entity);
        }
        return mapper.toDomain(publications.saveAndFlush(entity));
    }

    @Override
    public Optional<Publication> findById(Long id) {
        return publications.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Publication> findByIdForUpdate(Long id) {
        return lockedEntity(id).map(mapper::toDomain);
    }

    @Override
    public void delete(Long id, Long deletedByUserId, Instant deletedAt) {
        if (deletedByUserId == null || deletedByUserId <= 0 || deletedAt == null) {
            throw new IllegalArgumentException("A valid deletion actor and instant are required.");
        }
        PublicationEntity entity = lockedEntity(id)
                .orElseThrow(() -> new PublicationAlreadyDeletedException(id));
        history.saveAndFlush(historyMapper.snapshot(entity, deletedByUserId, deletedAt));
        comments.deleteAll(comments.findAllByPublication_Id(id));
        comments.flush();
        likes.deleteAll(likes.findAllByPublication_Id(id));
        likes.flush();
        publications.delete(entity);
        publications.flush();
    }

    private Optional<PublicationEntity> lockedEntity(Long id) {
        return publications.lockRootId(id).flatMap(publications::findById);
    }
}
