package com.jeepclub.backend.iam.authorization.infra.persistence.mapper;

import com.jeepclub.backend.iam.authorization.core.domain.model.Role;
import com.jeepclub.backend.iam.authorization.infra.persistence.entity.RoleEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class RoleMapper {

    public Role toDomain(RoleEntity entity) {
        Objects.requireNonNull(entity, "RoleEntity cannot be null");

        return Role.reconstitute(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getKind(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt()
        );
    }

    public RoleEntity toEntity(Role role) {
        Objects.requireNonNull(role, "Role cannot be null");

        return new RoleEntity(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.getKind(),
                role.getStatus(),
                role.getCreatedAt(),
                role.getUpdatedAt(),
                role.getDeletedAt()
        );
    }
}