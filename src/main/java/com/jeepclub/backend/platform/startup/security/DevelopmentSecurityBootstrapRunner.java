package com.jeepclub.backend.platform.startup.security;

import com.jeepclub.backend.platform.startup.properties.AdminBootstrapProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Order(2)
public class DevelopmentSecurityBootstrapRunner implements ApplicationRunner {

    private final DevelopmentSecurityBootstrapService developmentSecurityBootstrapService;
    private final AdminBootstrapProperties adminBootstrapProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (!adminBootstrapProperties.enabled()) {
            return;
        }

        developmentSecurityBootstrapService.bootstrap();
    }
}