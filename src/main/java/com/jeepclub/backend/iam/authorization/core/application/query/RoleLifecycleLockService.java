package com.jeepclub.backend.iam.authorization.core.application.query;
import com.jeepclub.backend.iam.authorization.api.module.role.RoleLifecycleLock;
import com.jeepclub.backend.iam.authorization.core.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service @RequiredArgsConstructor @Transactional(propagation=Propagation.MANDATORY)
class RoleLifecycleLockService implements RoleLifecycleLock {
    private final RoleRepository roles;
    private final UserRoleRepository userRoles;
    @Override public boolean isActive(Long id) {
        return id != null && roles.findByIdForUpdate(id).map(role -> role.isActive()).orElse(false);
    }
    @Override public boolean hasRootRole(Long userId) {
        return userId != null && roles.findRootForUpdate().filter(role -> role.isActive())
                .map(role -> userRoles.existsByUserIdAndRoleIdForUpdate(userId, role.getId())).orElse(false);
    }
}
