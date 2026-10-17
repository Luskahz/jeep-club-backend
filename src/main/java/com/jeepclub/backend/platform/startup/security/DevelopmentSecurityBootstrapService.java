package com.jeepclub.backend.platform.startup.security;

import com.jeepclub.backend.iam.identity.core.application.service.bootstrap.DevelopmentAdminUserBootstrapService;
import com.jeepclub.backend.iam.authorization.core.application.service.bootstrap.RootRoleBootstrapService;
import com.jeepclub.backend.iam.authorization.core.application.service.bootstrap.PermissionSynchronizationService;
import com.jeepclub.backend.iam.authorization.core.application.service.bootstrap.UserRoleAssignmentService;
import com.jeepclub.backend.iam.authorization.api.module.role.RoleQuery;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DevelopmentSecurityBootstrapService {

    private final PermissionSynchronizationService permissionSynchronizationService;
    private final DevelopmentAdminUserBootstrapService developmentAdminUserBootstrapService;
    private final RootRoleBootstrapService rootRoleBootstrapService;
    private final UserRoleAssignmentService userRoleAssignmentService;
    private final RoleQuery roleQuery;
    private final UserQuery userQuery;

    @Transactional
    public void bootstrap() {

        Long rootRoleId =
                rootRoleBootstrapService
                        .ensureRootRole();

        // CPF is mutable. An existing ROOT binding is the stable bootstrap identity;
        // claiming its former CPF must never grant another user ROOT on restart.
        if (roleQuery.findUserIdsByRoleId(rootRoleId).stream().anyMatch(userQuery::existsById)) {
            return;
        }

        Long adminUserId = developmentAdminUserBootstrapService.createAdminUserIfMissing();

        userRoleAssignmentService.assignRoleToUserIfMissing(
                adminUserId,
                rootRoleId
        );
    }
}
