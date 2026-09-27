package com.jeepclub.backend.publications.infra.persistence.jpa;

import com.jeepclub.backend.publications.infra.persistence.entity.PublicationCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PublicationCommentJpaRepository extends JpaRepository<PublicationCommentEntity, Long> {
    List<PublicationCommentEntity> findAllByPublication_Id(Long publicationId);
}
