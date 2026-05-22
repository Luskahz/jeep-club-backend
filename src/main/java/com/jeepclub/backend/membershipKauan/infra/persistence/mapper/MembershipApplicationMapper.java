package com.jeepclub.backend.membershipKauan.infra.persistence.mapper;

import com.jeepclub.backend.membershipKauan.core.domain.model.MembershipApplication;
import com.jeepclub.backend.membershipKauan.infra.persistence.entity.MembershipApplicationEntity;
import org.springframework.stereotype.Component;

@Component
public class MembershipApplicationMapper {

    public MembershipApplication toDomain(MembershipApplicationEntity entity) {
        if (entity == null) {
            return null;
        }

        return MembershipApplication.reconstitute(
                entity.getId(),
                entity.getName(),
                entity.getCpf(),
                entity.getEmail(),
                entity.getPhoneNumber(),
                entity.getMessage(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public MembershipApplicationEntity toEntity(MembershipApplication application) {
        if (application == null) {
            return null;
        }

        MembershipApplicationEntity entity = new MembershipApplicationEntity();

        entity.setId(application.getId());
        entity.setName(application.getName());
        entity.setCpf(application.getCpf());
        entity.setEmail(application.getEmail());
        entity.setPhoneNumber(application.getPhoneNumber());
        entity.setMessage(application.getMessage());
        entity.setStatus(application.getStatus());
        entity.setCreatedAt(application.getCreatedAt());
        entity.setUpdatedAt(application.getUpdatedAt());

        return entity;
    }
}