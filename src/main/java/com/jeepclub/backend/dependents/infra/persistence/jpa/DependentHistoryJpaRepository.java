package com.jeepclub.backend.dependents.infra.persistence.jpa;

import com.jeepclub.backend.dependents.infra.persistence.entity.DependentHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DependentHistoryJpaRepository
        extends JpaRepository<DependentHistoryEntity, Long> {
}