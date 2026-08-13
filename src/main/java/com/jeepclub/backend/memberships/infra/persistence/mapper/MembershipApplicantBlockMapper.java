package com.jeepclub.backend.memberships.infra.persistence.mapper;

import com.jeepclub.backend.memberships.core.domain.model.MembershipApplicantBlock;
import com.jeepclub.backend.memberships.infra.persistence.entity.MembershipApplicantBlockEntity;

public class MembershipApplicantBlockMapper {

    private MembershipApplicantBlockMapper() {}

    public static MembershipApplicantBlock toDomain(MembershipApplicantBlockEntity entity) {
        if (entity == null) return null;

        return MembershipApplicantBlock.reconstitute(
                entity.getId(),
                entity.getCpf(),
                entity.getReason(),
                entity.getBlockedAt(),
                entity.getBlockedByUserId(),
                entity.getUnblockedAt(),
                entity.getUnblockedByUserId()
        );
    }

    public static MembershipApplicantBlockEntity toEntity(MembershipApplicantBlock domain) {
        if (domain == null) return null;

        MembershipApplicantBlockEntity entity = new MembershipApplicantBlockEntity();
        entity.setId(domain.getId());
        entity.setCpf(domain.getCpf());
        entity.setActiveCpf(domain.getUnblockedAt() == null ? domain.getCpf() : null);
        entity.setReason(domain.getReason());
        entity.setBlockedAt(domain.getBlockedAt());
        entity.setBlockedByUserId(domain.getBlockedByUserId());
        entity.setUnblockedAt(domain.getUnblockedAt());
        entity.setUnblockedByUserId(domain.getUnblockedByUserId());
        return entity;
    }
}
