package com.jeepclub.backend.authorization.core.application.service;

import com.jeepclub.backend.authorization.core.application.exception.PermissionNotFoundException;
import com.jeepclub.backend.authorization.core.application.result.FindAllPermissionsResult;
import com.jeepclub.backend.authorization.core.application.result.permission.FindPermissionResult;
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
    public FindAllPermissionsResult findAllPermissions() {
        List<Permission> permissions = permissionRepository.findAll();

        return new FindAllPermissionsResult(permissions);
    }

    @Transactional(readOnly = true)
    public FindPermissionResult findPermissionById(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new PermissionNotFoundException(id));

        return new FindPermissionResult(permission);
    }
    @Transactional(readOnly = true)
    public FindPermissionResult findPermissionByCode(String rawCode) {
        PermissionCode code = parsePermissionCode(rawCode);

        Permission permission = permissionRepository.findByCode(code)
                .orElseThrow(() -> new PermissionNotFoundException(rawCode));

        return new FindPermissionResult(permission);
    }

    private PermissionCode parsePermissionCode(String rawCode) {
        try {
            return PermissionCode.valueOf(rawCode.trim().toUpperCase());
        } catch (RuntimeException exception) {
            throw new PermissionNotFoundException(rawCode);
        }
    }
}