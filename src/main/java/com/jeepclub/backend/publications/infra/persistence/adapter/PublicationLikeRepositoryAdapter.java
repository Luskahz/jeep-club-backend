package com.jeepclub.backend.publications.infra.persistence.adapter;

import com.jeepclub.backend.publications.core.domain.model.PublicationLike;
import com.jeepclub.backend.publications.core.repository.PublicationLikeRepository;
import com.jeepclub.backend.publications.infra.persistence.entity.PublicationLikeEntity;
import com.jeepclub.backend.publications.infra.persistence.jpa.PublicationLikeJpaRepository;
import com.jeepclub.backend.publications.infra.persistence.jpa.PublicationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PublicationLikeRepositoryAdapter implements PublicationLikeRepository {
    private final PublicationLikeJpaRepository likes;
    private final PublicationJpaRepository publications;

    @Override
    public PublicationLike save(PublicationLike like) {
        var entity = new PublicationLikeEntity();
        entity.setId(like.id());
        entity.setPublication(publications.getReferenceById(like.publicationId()));
        entity.setMemberUserId(like.memberUserId());
        entity.setCreatedAt(like.createdAt());
        var saved = likes.saveAndFlush(entity);
        return new PublicationLike(saved.getId(), like.publicationId(), saved.getMemberUserId(), saved.getCreatedAt());
    }
}
