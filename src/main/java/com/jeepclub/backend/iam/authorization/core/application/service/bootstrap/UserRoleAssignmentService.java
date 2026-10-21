package com.jeepclub.backend.iam.authorization.core.application.service.bootstrap;

import com.jeepclub.backend.iam.authorization.core.domain.model.UserRole;
import com.jeepclub.backend.iam.authorization.core.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserRoleAssignmentService {

    private final UserRoleRepository userRoleRepository;
    private final Clock clock;
    private final com.jeepclub.backend.iam.identity.api.module.UserIdentityLock identities;
    private final com.jeepclub.backend.iam.authorization.core.repository.RoleRepository roles;

    @Transactional
    public void assignRoleToUserIfMissing(
            Long userId,
            Long roleId
    ) {
        identities.byId(userId).orElseThrow(() ->
                new com.jeepclub.backend.iam.authorization.core.application.exception.userrole.AuthorizationUserNotFoundException(userId));
        roles.findByIdForUpdate(roleId).orElseThrow(() ->
                new com.jeepclub.backend.iam.authorization.core.application.exception.role.RoleNotFoundException(roleId)).ensureActive();
        Instant now = Instant.now(clock);
        boolean alreadyAssigned = userRoleRepository.existsByUserIdAndRoleIdForUpdate(
                userId,
                roleId
        );

        if (alreadyAssigned) {
            return;
        }

        UserRole userRole = UserRole.create(userId, roleId, now);

        userRoleRepository.save(
                userRole
        );
    }
}
