package com.jeepclub.backend.health.infra.persistence.jpa;

import com.jeepclub.backend.health.infra.persistence.entity.MedicalProfileAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalProfileAuditJpaRepository
        extends JpaRepository<MedicalProfileAuditEntity, Long> {
}
