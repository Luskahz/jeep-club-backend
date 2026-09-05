package com.jeepclub.backend.iam.authorization.core.application.service.userrole;

import com.jeepclub.backend.iam.authorization.core.application.exception.userrole.AuthorizationUserNotFoundException;
import com.jeepclub.backend.iam.authorization.core.application.exception.role.RoleNotFoundException;
import com.jeepclub.backend.iam.authorization.core.application.exception.role.RootRoleCannotBeManagedManuallyException;
import com.jeepclub.backend.iam.authorization.core.application.exception.userrole.UserRoleAlreadyExistsException;
import com.jeepclub.backend.iam.authorization.core.application.exception.userrole.UserRoleNotFoundException;
import com.jeepclub.backend.iam.authorization.core.application.result.RolesResult;
import com.jeepclub.backend.iam.authorization.core.domain.model.Role;
import com.jeepclub.backend.iam.authorization.core.domain.model.UserRole;
import com.jeepclub.backend.iam.authorization.core.port.UserIdentityPort;
import com.jeepclub.backend.iam.authorization.core.repository.RoleRepository;
import com.jeepclub.backend.iam.authorization.core.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminUserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final UserIdentityPort userIdentityPort;
    private final Clock clock;

    @Transactional(readOnly = true)
    public RolesResult findRolesByUserId(Long userId) {
        ensureUserExists(userId);

        List<Role> roles = userRoleRepository.findRolesByUserId(userId);

        return new RolesResult(roles);
    }

    @Transactional
    public void assignRoleToUser(
            Long userId,
            Long roleId
    ) {
        ensureUserExists(userId);

        Role role = findRole(roleId);

        ensureRoleCanBeManagedManually(role);
        role.ensureActive();

        boolean alreadyAssigned =
                userRoleRepository.existsByUserIdAndRoleId(
                        userId,
                        role.getId()
                );

        if (alreadyAssigned) {
            throw new UserRoleAlreadyExistsException(
                    userId,
                    role.getId()
            );
        }

        Instant now = Instant.now(clock);

        UserRole userRole = UserRole.create(
                userId,
                role.getId(),
                now
        );

        userRoleRepository.save(userRole);
    }

    @Transactional
    public void revokeRoleFromUser(
            Long userId,
            Long roleId
    ) {
        ensureUserExists(userId);

        Role role = findRole(roleId);

        ensureRoleCanBeManagedManually(role);

        boolean assigned =
                userRoleRepository.existsByUserIdAndRoleId(
                        userId,
                        role.getId()
                );

        if (!assigned) {
            throw new UserRoleNotFoundException(
                    userId,
                    role.getId()
            );
        }

        userRoleRepository.deleteByUserIdAndRoleId(
                userId,
                role.getId()
        );
    }

    @Transactional
    public void replaceUserRoles(
            Long userId,
            List<Long> roleIds
    ) {
        ensureUserExists(userId);
        Objects.requireNonNull(roleIds, "roleIds cannot be null");

        Set<Long> uniqueRoleIds = new HashSet<>(roleIds);

        List<Role> requestedRoles = uniqueRoleIds.stream()
                .map(this::findRole)
                .toList();

        requestedRoles.forEach(this::ensureRoleCanBeManagedManually);
        requestedRoles.forEach(Role::ensureActive);

        List<Role> currentRoles =
                userRoleRepository.findRolesByUserId(userId);

        currentRoles.stream()
                .filter(Role::isCustom)
                .forEach(role ->
                        userRoleRepository.deleteByUserIdAndRoleId(
                                userId,
                                role.getId()
                        )
                );

        if (requestedRoles.isEmpty()) {
            return;
        }

        Instant now = Instant.now(clock);

        List<UserRole> userRoles = requestedRoles.stream()
                .map(role -> UserRole.create(
                        userId,
                        role.getId(),
                        now
                ))
                .toList();

        userRoleRepository.saveAll(userRoles);
    }

    private Role findRole(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException(roleId));
    }

    private void ensureRoleCanBeManagedManually(Role role) {
        if (role.isRoot()) {
            throw new RootRoleCannotBeManagedManuallyException(
                    role.getId()
            );
        }
    }

    private void ensureUserExists(Long userId) {
        if (!userIdentityPort.existsById(userId)) {
            throw new AuthorizationUserNotFoundException(userId);
        }
    }
}