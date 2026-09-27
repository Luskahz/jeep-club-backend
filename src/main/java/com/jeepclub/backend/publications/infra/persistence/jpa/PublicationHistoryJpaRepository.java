package com.jeepclub.backend.publications.infra.persistence.jpa;

import com.jeepclub.backend.publications.infra.persistence.entity.PublicationHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicationHistoryJpaRepository extends JpaRepository<PublicationHistoryEntity, Long> { }
