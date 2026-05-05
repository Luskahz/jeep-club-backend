package com.jeepclub.backend.authorization.infra.config;

import com.jeepclub.backend.authorization.core.application.service.PermissionSynchronizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PermissionSynchronizationRunner implements ApplicationRunner {

    private final PermissionSynchronizationService permissionSynchronizationService;

    @Override
    public void run(ApplicationArguments args) {
        permissionSynchronizationService.synchronizePermissions();
    }
}