package com.jeepclub.backend.health.core.application.query;
import com.jeepclub.backend.health.api.module.medicalprofile.*;
import com.jeepclub.backend.health.core.repository.MedicalProfileCoverageRepository;
@org.springframework.stereotype.Service @lombok.RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly=true)
public class MedicalProfileCoverageQueryService implements MedicalProfileCoverageQuery {
    private final MedicalProfileCoverageRepository repository;
    public java.util.Set<Long> findCovered(MedicalProfileOwner type,java.util.Collection<Long> ids) {
        if(ids.isEmpty())return java.util.Set.of();
        if(ids.size()>500)throw new IllegalArgumentException("Maximum batch: 500");
        return repository.findCovered(type,ids);
    }
}
