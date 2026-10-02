package com.jeepclub.backend.publications.infra.persistence.jpa;

import com.jeepclub.backend.publications.infra.persistence.entity.PublicationLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PublicationLikeJpaRepository extends JpaRepository<PublicationLikeEntity, Long> {
    List<PublicationLikeEntity> findAllByPublication_Id(Long publicationId);
    java.util.Optional<PublicationLikeEntity> findByPublication_IdAndMemberUserId(Long publicationId, Long memberUserId);
    @org.springframework.data.jpa.repository.Query("select l.publication.id, count(l.id) from PublicationLikeEntity l where l.publication.id in :ids group by l.publication.id")
    List<Object[]> countByPublicationIds(java.util.Collection<Long> ids);
    @org.springframework.data.jpa.repository.Query("select l.publication.id from PublicationLikeEntity l where l.publication.id in :ids and l.memberUserId = :memberUserId")
    java.util.Set<Long> findLikedPublicationIds(java.util.Collection<Long> ids, Long memberUserId);
}
