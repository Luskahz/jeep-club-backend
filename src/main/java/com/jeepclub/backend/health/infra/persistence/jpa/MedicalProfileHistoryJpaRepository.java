package com.jeepclub.backend.health.infra.persistence.jpa;

import com.jeepclub.backend.health.infra.persistence.entity.MedicalProfileHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalProfileHistoryJpaRepository
        extends JpaRepository<MedicalProfileHistoryEntity, Long> {
}
