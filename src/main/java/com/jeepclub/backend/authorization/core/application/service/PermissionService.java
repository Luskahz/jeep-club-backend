package com.jeepclub.backend.authorization.core.application.service;


import com.jeepclub.backend.authorization.core.application.exception.PermissionNotFoundException;
import com.jeepclub.backend.authorization.core.application.result.FindAllPermissionsResult;
import com.jeepclub.backend.authorization.core.application.result.FindPermissionResult;
import com.jeepclub.backend.authorization.core.domain.enums.PermissionDefinition;
import com.jeepclub.backend.authorization.core.domain.model.Permission;
import com.jeepclub.backend.authorization.core.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;


    @Transactional(readOnly = true)
    public FindAllPermissionsResult findAllPersistedPermissions(){
        List<Permission> permissions = permissionRepository.findAll();
            return new FindAllPermissionsResult(permissions);
    }


    @Transactional(readOnly = true)
    public FindPermissionResult findPersistedPermissionById(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new PermissionNotFoundException(id));

        return new FindPermissionResult(permission);
    }


    @Transactional(readOnly = true)
    public FindAllPermissionsResult findAllPermissionsFromCatalog() {
        List<Permission> permissions = Arrays.stream(PermissionDefinition.values())
                .map(Permission::fromDefinition)
                .toList();

        return new FindAllPermissionsResult(permissions);
    }

    @Transactional(readOnly = true)
    public FindPermissionResult findPermissionByCodeFromCatalog(String rawCode) {
        PermissionDefinition definition = Arrays.stream(PermissionDefinition.values())
                .filter(item -> item.getCode().name().equals(rawCode))
                .findFirst()
                .orElseThrow(() -> new PermissionNotFoundException(rawCode));

        Permission permission = Permission.fromDefinition(definition);

        return new FindPermissionResult(permission);
    }
}
