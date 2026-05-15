package com.jeepclub.backend.authorization.infra.persistence.mapper;

import com.jeepclub.backend.authorization.core.domain.model.UserRole;
import com.jeepclub.backend.authorization.infra.persistence.entity.RoleEntity;
import com.jeepclub.backend.authorization.infra.persistence.entity.UserRoleEntity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class UserRoleMapper {

    private final EntityManager entityManager;

    public UserRole toDomain(UserRoleEntity entity) {
        Objects.requireNonNull(entity, "UserRoleEntity cannot be null");

        return UserRole.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getRole().getId(),
                entity.getCreatedAt()
        );
    }

    public UserRoleEntity toEntity(UserRole userRole) {
        Objects.requireNonNull(userRole, "UserRole cannot be null");

        RoleEntity roleReference = entityManager.getReference(
                RoleEntity.class,
                userRole.getRoleId()
        );

        return new UserRoleEntity(
                userRole.getId(),
                userRole.getUserId(),
                roleReference,
                userRole.getCreatedAt()
        );
    }
}