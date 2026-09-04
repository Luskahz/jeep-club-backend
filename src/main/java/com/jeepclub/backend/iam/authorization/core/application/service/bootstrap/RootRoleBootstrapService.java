package com.jeepclub.backend.iam.authorization.core.application.service.bootstrap;

import com.jeepclub.backend.iam.authorization.core.domain.model.Permission;
import com.jeepclub.backend.iam.authorization.core.domain.model.Role;
import com.jeepclub.backend.iam.authorization.core.domain.model.RolePermission;
import com.jeepclub.backend.iam.authorization.core.repository.PermissionRepository;
import com.jeepclub.backend.iam.authorization.core.repository.RolePermissionRepository;
import com.jeepclub.backend.iam.authorization.core.repository.RoleRepository;
import com.jeepclub.backend.shared.bootstrap.AdminBootstrapConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RootRoleBootstrapService {
    private static final String ROOT_ROLE_DESCRIPTION =
            "Role raiz do sistema";

    private final AdminBootstrapConfig adminBootstrapConfig;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final Clock clock;

    @Transactional
    public Long ensureRootRole() {
        Role root = roleRepository.findRoot()
                .orElseGet(this::createRootRole);

        assignAllPermissions(root.getId());

        return root.getId();
    }

    private Role createRootRole() {
        Instant now = Instant.now(clock);

        Role root = Role.createRoot(
                adminBootstrapConfig.roleName(),
                ROOT_ROLE_DESCRIPTION,
                now
        );

        return roleRepository.save(root);
    }

    private void assignAllPermissions(Long rootRoleId) {
        Instant now = Instant.now(clock);

        List<Permission> permissions = permissionRepository.findAll();

        for (Permission permission : permissions) {
            assignPermissionIfMissing(
                    rootRoleId,
                    permission.getId(),
                    now
            );
        }
    }

    private void assignPermissionIfMissing(
            Long roleId,
            Long permissionId,
            Instant now
    ) {
        boolean alreadyAssigned =
                rolePermissionRepository.existsByRoleIdAndPermissionId(
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
