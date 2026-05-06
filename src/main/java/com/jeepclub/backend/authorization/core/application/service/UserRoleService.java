package com.jeepclub.backend.authorization.core.application.service;

import com.jeepclub.backend.authorization.core.application.exception.RoleNotFoundException;
import com.jeepclub.backend.authorization.core.application.exception.UserRoleAlreadyExistsException;
import com.jeepclub.backend.authorization.core.application.result.FindAllRolesResult;
import com.jeepclub.backend.authorization.core.domain.model.Role;
import com.jeepclub.backend.authorization.core.domain.model.UserRole;
import com.jeepclub.backend.authorization.core.repository.RoleRepository;
import com.jeepclub.backend.authorization.core.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public FindAllRolesResult findRolesByUserId(Long userId) {
        List<Role> roles = userRoleRepository.findRolesByUserId(userId);

        return new FindAllRolesResult(roles);
    }

    @Transactional
    public void assignRoleToUser(
            Long userId,
            Long roleId
    ) {
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

        Instant now = Instant.now();

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
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException(roleId));

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
        Set<Long> uniqueRoleIds = new HashSet<>(roleIds);

        List<Role> roles = uniqueRoleIds.stream()
                .map(roleId -> roleRepository.findById(roleId)
                        .orElseThrow(() -> new RoleNotFoundException(roleId)))
                .toList();

        roles.forEach(Role::ensureActive);

        userRoleRepository.deleteByUserId(userId);

        Instant now = Instant.now();

        List<UserRole> userRoles = roles.stream()
                .map(role -> UserRole.create(
                        userId,
                        role.getId(),
                        now
                ))
                .toList();

        userRoleRepository.saveAll(userRoles);
    }
}