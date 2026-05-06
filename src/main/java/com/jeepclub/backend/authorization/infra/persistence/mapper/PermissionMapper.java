package com.jeepclub.backend.authorization.infra.persistence.mapper;

import com.jeepclub.backend.authorization.core.domain.model.Permission;
import com.jeepclub.backend.authorization.infra.persistence.entity.PermissionEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class PermissionMapper {

    public Permission toDomain(PermissionEntity entity) {
        Objects.requireNonNull(entity, "PermissionEntity cannot be null");

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
        Objects.requireNonNull(permission, "Permission cannot be null");

        return new PermissionEntity(
                permission.getId(),
                permission.getCode(),
                permission.getDescription(),
                permission.getModule(),
                permission.getCreatedAt(),
                permission.getUpdatedAt()
        );
    }
}