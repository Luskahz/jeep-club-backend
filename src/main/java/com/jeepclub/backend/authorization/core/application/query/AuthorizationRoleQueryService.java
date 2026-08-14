package com.jeepclub.backend.authorization.core.application.query;

import com.jeepclub.backend.authorization.api.module.role.RoleQuery;
import com.jeepclub.backend.authorization.core.domain.model.Role;
import com.jeepclub.backend.authorization.core.repository.RoleRepository;
import com.jeepclub.backend.authorization.core.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
class AuthorizationRoleQueryService implements RoleQuery {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveRoleById(Long roleId) {
        Objects.requireNonNull(roleId, "roleId cannot be null");

        return roleRepository.findById(roleId)
                .map(Role::isActive)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findUserIdsByRoleId(Long roleId) {
        Objects.requireNonNull(roleId, "roleId cannot be null");

        if (!existsActiveRoleById(roleId)) {
            return List.of();
        }

        return userRoleRepository.findUserIdsByRoleId(roleId);
    }
}
