package com.jeepclub.backend.platform.startup.security;

import com.jeepclub.backend.identity.core.application.service.bootstrap.DevelopmentAdminUserBootstrapService;
import com.jeepclub.backend.authorization.core.application.service.bootstrap.RootRoleBootstrapService;
import com.jeepclub.backend.authorization.core.application.service.bootstrap.PermissionSynchronizationService;
import com.jeepclub.backend.authorization.core.application.service.bootstrap.UserRoleAssignmentService;
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

    @Transactional
    public void bootstrap() {

        Long adminUserId =
                developmentAdminUserBootstrapService
                        .createAdminUserIfMissing();

        Long rootRoleId =
                rootRoleBootstrapService
                        .ensureRootRole();


        userRoleAssignmentService.assignRoleToUserIfMissing(
                adminUserId,
                rootRoleId
        );
    }
}
