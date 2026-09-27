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
    @Override
    public java.util.Optional<PublicationLike> findByPublicationAndMember(Long publicationId, Long memberUserId) {
        return likes.findByPublication_IdAndMemberUserId(publicationId, memberUserId)
                .map(e -> new PublicationLike(e.getId(), publicationId, e.getMemberUserId(), e.getCreatedAt()));
    }

    @Override
    public void delete(PublicationLike like) { likes.deleteById(like.id()); }

    @Override
    public java.util.Map<Long, Long> counts(java.util.Collection<Long> publicationIds) {
        if (publicationIds.isEmpty()) return java.util.Map.of();
        var result = new java.util.HashMap<Long, Long>();
        likes.countByPublicationIds(publicationIds).forEach(row -> result.put((Long) row[0], (Long) row[1]));
        return java.util.Map.copyOf(result);
    }

    @Override
    public java.util.Set<Long> likedByMember(java.util.Collection<Long> publicationIds, Long memberUserId) {
        return publicationIds.isEmpty() ? java.util.Set.of() : likes.findLikedPublicationIds(publicationIds, memberUserId);
    }
}
