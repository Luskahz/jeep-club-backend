package com.jeepclub.backend.authorization.infra.persistence.repository.jpa;

import com.jeepclub.backend.authorization.core.domain.model.Permission;
import com.jeepclub.backend.authorization.core.domain.model.RolePermission;
import com.jeepclub.backend.authorization.core.repository.RolePermissionRepository;
import com.jeepclub.backend.authorization.infra.persistence.entity.RolePermissionEntity;
import com.jeepclub.backend.authorization.infra.persistence.jpa.RolePermissionJpaRepository;
import com.jeepclub.backend.authorization.infra.persistence.mapper.PermissionMapper;
import com.jeepclub.backend.authorization.infra.persistence.mapper.RolePermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RolePermissionRepositoryJpa implements RolePermissionRepository {

    private final RolePermissionJpaRepository jpa;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;

    @Override
    public RolePermission save(RolePermission rolePermission) {
        RolePermissionEntity entity = rolePermissionMapper.toEntity(rolePermission);

        RolePermissionEntity savedEntity = jpa.save(entity);

        return rolePermissionMapper.toDomain(savedEntity);
    }

    @Override
    public boolean existsByRoleIdAndPermissionId(
            Long roleId,
            Long permissionId
    ) {
        return jpa.existsByRoleIdAndPermissionId(roleId, permissionId);
    }

    @Override
    public List<Permission> findPermissionsByRoleId(Long roleId) {
        return jpa.findPermissionsByRoleId(roleId)
                .stream()
                .map(permissionMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteByRoleIdAndPermissionId(
            Long roleId,
            Long permissionId
    ) {
        jpa.deleteByRoleIdAndPermissionId(roleId, permissionId);
    }
}