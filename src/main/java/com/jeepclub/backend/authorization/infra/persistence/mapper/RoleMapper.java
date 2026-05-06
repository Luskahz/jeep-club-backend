package com.jeepclub.backend.authorization.infra.persistence.mapper;

import com.jeepclub.backend.authorization.core.domain.model.Role;
import com.jeepclub.backend.authorization.infra.persistence.entity.RoleEntity;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper {

    public Role toDomain(RoleEntity entity) {
        if (entity == null) {
            return null;
        }

        return Role.reconstitute(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt()
        );
    }

    public RoleEntity toEntity(Role role) {
        if (role == null) {
            return null;
        }

        return new RoleEntity(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.getStatus(),
                role.getCreatedAt(),
                role.getUpdatedAt(),
                role.getDeletedAt()
        );
    }
}