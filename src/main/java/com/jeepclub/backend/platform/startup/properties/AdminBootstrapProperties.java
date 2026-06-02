package com.jeepclub.backend.platform.startup.properties;

import com.jeepclub.backend.shared.bootstrap.AdminBootstrapConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDate;

@ConfigurationProperties(prefix = "security.bootstrap.admin")
public record AdminBootstrapProperties(
        boolean enabled,
        String name,
        LocalDate birthDate,
        String email,
        String cpf,
        String rg,
        String password,
        String phoneNumber,
        String roleName
) implements AdminBootstrapConfig {
}