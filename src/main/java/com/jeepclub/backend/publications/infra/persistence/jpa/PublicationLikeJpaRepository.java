package com.jeepclub.backend.publications.infra.persistence.jpa;

import com.jeepclub.backend.publications.infra.persistence.entity.PublicationLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PublicationLikeJpaRepository extends JpaRepository<PublicationLikeEntity, Long> {
    List<PublicationLikeEntity> findAllByPublication_Id(Long publicationId);
}
