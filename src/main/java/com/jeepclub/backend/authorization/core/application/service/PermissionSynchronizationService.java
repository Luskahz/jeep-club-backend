package com.jeepclub.backend.authorization.core.application.service;

import com.jeepclub.backend.authorization.core.domain.enums.PermissionDefinition;
import com.jeepclub.backend.authorization.core.domain.model.Permission;
import com.jeepclub.backend.authorization.core.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class PermissionSynchronizationService {

    private final PermissionRepository permissionRepository;

    @Transactional
    public void synchronizePermissions() {
        Arrays.stream(PermissionDefinition.values())
                .forEach(this::synchronizePermission);
    }

    private void synchronizePermission(PermissionDefinition definition) {
        permissionRepository.findByCode(definition.getCode())
                .ifPresentOrElse(
                        existingPermission -> updateExistingPermissionIfNeeded(existingPermission, definition),
                        () -> createPermission(definition)
                );
    }

    private void updateExistingPermissionIfNeeded(
            Permission existingPermission,
            PermissionDefinition definition
    ) {
        boolean changed = existingPermission.synchronizeWith(definition, Instant.now());

        if (changed) {
            permissionRepository.save(existingPermission);
        }
    }

    private void createPermission(PermissionDefinition definition) {
        Permission permission = Permission.fromDefinition(definition);

        permissionRepository.save(permission);
    }
}