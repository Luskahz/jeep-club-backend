package com.jeepclub.backend.memberships.infra.persistence.mapper;

import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import com.jeepclub.backend.memberships.infra.persistence.entity.MembershipApplicationEntity;

public class MembershipApplicationMapper {

    private MembershipApplicationMapper() {}

    public static MembershipApplication toDomain(MembershipApplicationEntity entity) {
        if (entity == null) return null;

        return MembershipApplication.reconstitute(
                entity.getId(),
                entity.getName(),
                entity.getCpf(),
                entity.getEmail(),
                entity.getPhoneNumber(),
                entity.getMessage(),
                entity.getStatus(),
                entity.getRejectionReason(),
                entity.getReviewedByUserId(),
                entity.getCreatedUserId(),
                entity.getRequestedAt(),
                entity.getReviewedAt(),
                entity.getFinishedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }

    public static MembershipApplicationEntity toEntity(MembershipApplication domain) {
        if (domain == null) return null;

        MembershipApplicationEntity entity = new MembershipApplicationEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setCpf(domain.getCpf());
        entity.setEmail(domain.getEmail());
        entity.setPhoneNumber(domain.getPhoneNumber());
        entity.setMessage(domain.getMessage());
        entity.setStatus(domain.getStatus());
        entity.setRejectionReason(domain.getRejectionReason());
        entity.setReviewedByUserId(domain.getReviewedByUserId());
        entity.setCreatedUserId(domain.getCreatedUserId());
        entity.setRequestedAt(domain.getRequestedAt());
        entity.setReviewedAt(domain.getReviewedAt());
        entity.setFinishedAt(domain.getFinishedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setVersion(domain.getVersion());
        return entity;
    }
}
