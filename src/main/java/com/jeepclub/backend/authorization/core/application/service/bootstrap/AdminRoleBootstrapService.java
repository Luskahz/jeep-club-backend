package com.jeepclub.backend.authorization.core.application.service.bootstrap;

import com.jeepclub.backend.authorization.core.domain.model.Permission;
import com.jeepclub.backend.authorization.core.domain.model.Role;
import com.jeepclub.backend.authorization.core.domain.model.RolePermission;
import com.jeepclub.backend.authorization.core.repository.PermissionRepository;
import com.jeepclub.backend.authorization.core.repository.RolePermissionRepository;
import com.jeepclub.backend.authorization.core.repository.RoleRepository;
import com.jeepclub.backend.shared.bootstrap.AdminBootstrapConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminRoleBootstrapService {

    private static final String ADMIN_ROLE_DESCRIPTION = "Role administrativa padrão do sistema";

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final AdminBootstrapConfig adminBootstrapConfig;
    private final Clock clock;

    @Transactional
    public Long createAdminRoleIfMissing() {
        return roleRepository.findByName(adminBootstrapConfig.roleName())
                .map(Role::getId)
                .orElseGet(this::createAdminRole);
    }

    @Transactional
    public void assignAllPermissionsToRole(Long roleId) {
        Instant now = Instant.now(clock);

        List<Permission> permissions = permissionRepository.findAll();

        for (Permission permission : permissions) {
            assignPermissionIfMissing(
                    roleId,
                    permission.getId(),
                    now
            );
        }
    }

    private Long createAdminRole() {
        Instant now = Instant.now(clock);

        Role role = Role.create(
                adminBootstrapConfig.roleName(),
                ADMIN_ROLE_DESCRIPTION,
                now
        );

        Role savedRole = roleRepository.save(role);

        return savedRole.getId();
    }

    private void assignPermissionIfMissing(
            Long roleId,
            Long permissionId,
            Instant now
    ) {
        boolean alreadyAssigned = rolePermissionRepository.existsByRoleIdAndPermissionId(
                roleId,
                permissionId
        );

        if (alreadyAssigned) {
            return;
        }

        RolePermission rolePermission = RolePermission.create(
                roleId,
                permissionId,
                now
        );

        rolePermissionRepository.save(rolePermission);
    }
}
