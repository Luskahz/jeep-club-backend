package com.jeepclub.backend.health.infra.persistence.adapter;

import com.jeepclub.backend.health.core.application.audit.MedicalProfileAuditEvent;
import com.jeepclub.backend.health.infra.persistence.entity.MedicalProfileAuditEntity;
import com.jeepclub.backend.health.infra.persistence.jpa.MedicalProfileAuditJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MedicalProfileAuditWriter {

    private final MedicalProfileAuditJpaRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(MedicalProfileAuditEvent event) {
        MedicalProfileAuditEntity entity = new MedicalProfileAuditEntity();
        entity.setActorUserId(event.actorUserId());
        entity.setOwnerType(event.ownerType());
        entity.setOwnerId(event.ownerId());
        entity.setOperation(event.operation());
        entity.setOutcome(event.outcome());
        entity.setOccurredAt(event.occurredAt());
        repository.save(entity);
    }
}
