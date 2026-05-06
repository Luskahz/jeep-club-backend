package com.jeepclub.backend.authorization.core.application.service;

import com.jeepclub.backend.authorization.core.application.exception.PermissionNotFoundException;
import com.jeepclub.backend.authorization.core.application.exception.RoleNotFoundException;
import com.jeepclub.backend.authorization.core.application.exception.RolePermissionAlreadyExistsException;
import com.jeepclub.backend.authorization.core.application.exception.RolePermissionNotFoundException;
import com.jeepclub.backend.authorization.core.application.result.FindAllPermissionsResult;
import com.jeepclub.backend.authorization.core.domain.model.Permission;
import com.jeepclub.backend.authorization.core.domain.model.Role;
import com.jeepclub.backend.authorization.core.domain.model.RolePermission;
import com.jeepclub.backend.authorization.core.repository.PermissionRepository;
import com.jeepclub.backend.authorization.core.repository.RolePermissionRepository;
import com.jeepclub.backend.authorization.core.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Transactional(readOnly = true)
    public FindAllPermissionsResult findPermissionsByRoleId(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException(roleId));

        List<Permission> permissions = rolePermissionRepository.findPermissionsByRoleId(role.getId());

        return new FindAllPermissionsResult(permissions);
    }

    @Transactional
    public void assignPermissionToRole(
            Long roleId,
            Long permissionId
    ) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException(roleId));

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new PermissionNotFoundException(permissionId));

        role.ensureActive();

        boolean alreadyAssigned = rolePermissionRepository.existsByRoleIdAndPermissionId(
                role.getId(),
                permission.getId()
        );

        if (alreadyAssigned) {
            throw new RolePermissionAlreadyExistsException(
                    role.getId(),
                    permission.getId()
            );
        }

        Instant now = Instant.now();

        RolePermission rolePermission = RolePermission.create(
                role.getId(),
                permission.getId(),
                now
        );

        rolePermissionRepository.save(rolePermission);
    }

    @Transactional
    public void removePermissionFromRole(
            Long roleId,
            Long permissionId
    ) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException(roleId));

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new PermissionNotFoundException(permissionId));

        role.ensureActive();

        boolean assigned = rolePermissionRepository.existsByRoleIdAndPermissionId(
                role.getId(),
                permission.getId()
        );

        if (!assigned) {
            throw new RolePermissionNotFoundException(
                    role.getId(),
                    permission.getId()
            );
        }

        rolePermissionRepository.deleteByRoleIdAndPermissionId(
                role.getId(),
                permission.getId()
        );
    }
}