package com.jeepclub.backend.authorization.core.application.service;

import com.jeepclub.backend.authorization.core.application.exception.RoleNotFoundException;
import com.jeepclub.backend.authorization.core.application.exception.UserNotFoundException;
import com.jeepclub.backend.authorization.core.application.exception.UserRoleAlreadyExistsException;
import com.jeepclub.backend.authorization.core.application.exception.UserRoleNotFoundException;
import com.jeepclub.backend.authorization.core.application.result.RolesResult;
import com.jeepclub.backend.authorization.core.domain.model.Role;
import com.jeepclub.backend.authorization.core.domain.model.UserRole;
import com.jeepclub.backend.authorization.core.port.UserIdentityProvider;
import com.jeepclub.backend.authorization.core.repository.RoleRepository;
import com.jeepclub.backend.authorization.core.repository.UserRoleRepository;
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
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final UserIdentityProvider userIdentityProvider;
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

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException(roleId));

        role.ensureActive();

        boolean alreadyAssigned = userRoleRepository.existsByUserIdAndRoleId(
                userId,
                role.getId()
        );

        if (alreadyAssigned) {
            throw new UserRoleAlreadyExistsException(userId, role.getId());
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

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException(roleId));

        boolean assigned = userRoleRepository.existsByUserIdAndRoleId(
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

        List<Role> roles = uniqueRoleIds.stream()
                .map(roleId -> roleRepository.findById(roleId)
                        .orElseThrow(() -> new RoleNotFoundException(roleId)))
                .toList();

        roles.forEach(Role::ensureActive);

        userRoleRepository.deleteByUserId(userId);

        Instant now = Instant.now(clock);

        List<UserRole> userRoles = roles.stream()
                .map(role -> UserRole.create(
                        userId,
                        role.getId(),
                        now
                ))
                .toList();

        userRoleRepository.saveAll(userRoles);
    }

    private void ensureUserExists(Long userId) {
        if (!userIdentityProvider.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
    }
}