package com.jeepclub.backend.authorization.infra.persistence.mapper;

import com.jeepclub.backend.authorization.core.domain.model.Permission;
import com.jeepclub.backend.authorization.infra.persistence.entity.PermissionEntity;
import org.springframework.stereotype.Component;

@Component
public class PermissionPersistenceMapper {

    public Permission toDomain(PermissionEntity entity) {
        return Permission.reconstitute(
                entity.getId(),
                entity.getCode(),
                entity.getDescription(),
                entity.getModule(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public PermissionEntity toEntity(Permission permission) {
        PermissionEntity entity = new PermissionEntity();

        entity.setId(permission.getId());
        entity.setCode(permission.getCode());
        entity.setDescription(permission.getDescription());
        entity.setModule(permission.getModule());
        entity.setCreatedAt(permission.getCreatedAt());
        entity.setUpdatedAt(permission.getUpdatedAt());

        return entity;
    }
}