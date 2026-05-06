package com.jeepclub.backend.authorization.core.application.service;

import com.jeepclub.backend.authorization.core.application.exception.PermissionNotFoundException;
import com.jeepclub.backend.authorization.core.application.result.PermissionResult;
import com.jeepclub.backend.authorization.core.application.result.PermissionsResult;
import com.jeepclub.backend.authorization.core.domain.enums.PermissionCode;
import com.jeepclub.backend.authorization.core.domain.model.Permission;
import com.jeepclub.backend.authorization.core.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public PermissionsResult findAllPermissions() {
        List<Permission> permissions = permissionRepository.findAll();

        return new PermissionsResult(permissions);
    }

    @Transactional(readOnly = true)
    public PermissionResult findPermissionById(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new PermissionNotFoundException(id));

        return new PermissionResult(permission);
    }
    @Transactional(readOnly = true)
    public PermissionResult findPermissionByCode(String rawCode) {
        PermissionCode code = parsePermissionCode(rawCode);

        Permission permission = permissionRepository.findByCode(code)
                .orElseThrow(() -> new PermissionNotFoundException(rawCode));

        return new PermissionResult(permission);
    }

    private PermissionCode parsePermissionCode(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new PermissionNotFoundException(rawCode);
        }

        try {
            return PermissionCode.valueOf(rawCode.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new PermissionNotFoundException(rawCode);
        }
    }
}