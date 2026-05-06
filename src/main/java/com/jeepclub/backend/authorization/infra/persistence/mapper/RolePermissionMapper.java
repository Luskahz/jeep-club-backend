package com.jeepclub.backend.authorization.infra.persistence.mapper;

import com.jeepclub.backend.authorization.core.domain.model.RolePermission;
import com.jeepclub.backend.authorization.infra.persistence.entity.PermissionEntity;
import com.jeepclub.backend.authorization.infra.persistence.entity.RoleEntity;
import com.jeepclub.backend.authorization.infra.persistence.entity.RolePermissionEntity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class RolePermissionMapper {

    private final EntityManager entityManager;

    public RolePermission toDomain(RolePermissionEntity entity) {
        Objects.requireNonNull(entity, "RolePermissionEntity cannot be null");

        return RolePermission.reconstitute(
                entity.getId(),
                entity.getRole().getId(),
                entity.getPermission().getId(),
                entity.getCreatedAt()
        );
    }

    public RolePermissionEntity toEntity(RolePermission rolePermission) {
        Objects.requireNonNull(rolePermission, "RolePermission cannot be null");

        RoleEntity roleReference = entityManager.getReference(
                RoleEntity.class,
                rolePermission.getRoleId()
        );

        PermissionEntity permissionReference = entityManager.getReference(
                PermissionEntity.class,
                rolePermission.getPermissionId()
        );

        return new RolePermissionEntity(
                rolePermission.getId(),
                roleReference,
                permissionReference,
                rolePermission.getCreatedAt()
        );
    }
}