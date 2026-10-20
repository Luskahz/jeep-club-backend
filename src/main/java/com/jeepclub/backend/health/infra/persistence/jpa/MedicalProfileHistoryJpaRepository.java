package com.jeepclub.backend.health.infra.persistence.jpa;

import com.jeepclub.backend.health.infra.persistence.entity.MedicalProfileHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalProfileHistoryJpaRepository
        extends JpaRepository<MedicalProfileHistoryEntity, Long> {

    @org.springframework.data.jpa.repository.Query(value="select id from medical_profiles_history where owner_type='DEPENDENT' and owner_id=:id limit 1 for update", nativeQuery=true)
    java.util.Optional<Long> lockDependentReference(@org.springframework.data.repository.query.Param("id") Long id);
}
