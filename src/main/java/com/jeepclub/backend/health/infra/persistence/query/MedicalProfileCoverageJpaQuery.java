package com.jeepclub.backend.health.infra.persistence.query;
import com.jeepclub.backend.health.core.repository.MedicalProfileCoverageRepository;
import com.jeepclub.backend.health.api.module.medicalprofile.MedicalProfileOwner;
@org.springframework.stereotype.Repository @lombok.RequiredArgsConstructor
public class MedicalProfileCoverageJpaQuery implements MedicalProfileCoverageRepository {
    private final jakarta.persistence.EntityManager em;
    public java.util.Set<Long> findCovered(MedicalProfileOwner type,java.util.Collection<Long> ids) {
        return new java.util.HashSet<>(em.createQuery("select m.ownerId from MedicalProfileEntity m where cast(m.ownerType as string)=:type and m.ownerId in :ids",Long.class)
            .setParameter("type",type.name()).setParameter("ids",ids).getResultList());
    }
}
