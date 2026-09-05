package com.jeepclub.backend.iam.authorization.core.application.service.bootstrap;

import com.jeepclub.backend.shared.authorization.PermissionDefinition;
import com.jeepclub.backend.iam.authorization.core.domain.model.Permission;
import com.jeepclub.backend.iam.authorization.core.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class PermissionSynchronizationService {

    private final PermissionRepository permissionRepository;
    private final Clock clock;

    @Transactional
    public void synchronizePermissions() {
        Instant synchronizedAt = Instant.now(clock);

        Arrays.stream(PermissionDefinition.values())
                .forEach(definition -> synchronizePermission(definition, synchronizedAt));
    }

    private void synchronizePermission(
            PermissionDefinition definition,
            Instant synchronizedAt
    ) {
        permissionRepository.findByCode(definition.getCode())
                .ifPresentOrElse(
                        existingPermission -> updateExistingPermissionIfNeeded(
                                existingPermission,
                                definition,
                                synchronizedAt
                        ),
                        () -> createPermission(definition, synchronizedAt)
                );
    }

    private void updateExistingPermissionIfNeeded(
            Permission existingPermission,
            PermissionDefinition definition,
            Instant synchronizedAt
    ) {
        boolean changed = existingPermission.synchronizeWith(
                definition,
                synchronizedAt
        );

        if (changed) {
            permissionRepository.save(existingPermission);
        }
    }

    private void createPermission(
            PermissionDefinition definition,
            Instant synchronizedAt
    ) {
        Permission permission = Permission.fromDefinition(
                definition,
                synchronizedAt
        );

        permissionRepository.save(permission);
    }
}
