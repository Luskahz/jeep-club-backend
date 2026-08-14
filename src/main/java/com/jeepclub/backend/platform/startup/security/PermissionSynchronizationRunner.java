package com.jeepclub.backend.platform.startup.security;

import com.jeepclub.backend.authorization.core.application.service.bootstrap.PermissionSynchronizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(1)
public class PermissionSynchronizationRunner implements ApplicationRunner {

    private final PermissionSynchronizationService permissionSynchronizationService;

    @Override
    public void run(ApplicationArguments args) {
        permissionSynchronizationService.synchronizePermissions();
    }
}
