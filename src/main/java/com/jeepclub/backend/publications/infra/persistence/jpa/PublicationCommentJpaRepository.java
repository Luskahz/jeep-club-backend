package com.jeepclub.backend.publications.infra.persistence.jpa;

import com.jeepclub.backend.publications.infra.persistence.entity.PublicationCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PublicationCommentJpaRepository extends JpaRepository<PublicationCommentEntity, Long> {
    List<PublicationCommentEntity> findAllByPublication_Id(Long publicationId);
    org.springframework.data.domain.Page<PublicationCommentEntity> findByPublication_Id(Long publicationId, org.springframework.data.domain.Pageable pageable);
    @org.springframework.data.jpa.repository.Query("select c.publication.id, count(c.id) from PublicationCommentEntity c where c.publication.id in :ids group by c.publication.id")
    List<Object[]> countByPublicationIds(java.util.Collection<Long> ids);
}
