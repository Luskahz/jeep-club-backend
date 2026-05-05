package com.jeepclub.backend.authorization.core.application.service;


import com.jeepclub.backend.authorization.core.application.result.FindAllPermissionsResult;
import com.jeepclub.backend.authorization.core.application.result.FindPermissionResult;
import com.jeepclub.backend.authorization.core.domain.model.Permission;
import com.jeepclub.backend.authorization.core.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;


    @Transactional(readOnly = true)
    public FindAllPermissionsResult findAllPermissions(){
        List<Permission> permissions = permissionRepository.findAll();
            return new FindAllPermissionsResult(permissions);
    }


    @Transactional(readOnly = true)
    public FindPermissionResult findPermissionById(Long id){
        Optional<Permission> optPermission = permissionRepository.findById(id);
        if(optPermission.isPresent()){
            return new FindPermissionResult(optPermission.get());
        } else{
            throw
        }
    }

}
