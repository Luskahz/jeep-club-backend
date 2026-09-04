package com.jeepclub.backend.identity.core.application.service.bootstrap;

import com.jeepclub.backend.identity.api.module.UserQuery;
import com.jeepclub.backend.identity.api.module.UserRegistration;
import com.jeepclub.backend.identity.api.module.UserRegistrationData;
import com.jeepclub.backend.identity.api.module.exception.UserRegistrationConflictException;
import com.jeepclub.backend.shared.bootstrap.AdminBootstrapConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DevelopmentAdminUserBootstrapService {

    private final UserQuery userQuery;
    private final UserRegistration userRegistration;
    private final AdminBootstrapConfig adminBootstrapConfig;
    private final Clock clock;

    public Long createAdminUserIfMissing() {
        return userQuery
                .findByCpf(adminBootstrapConfig.cpf())
                .map(user -> user.id())
                .orElseGet(
                        this::createAdminUserHandlingConcurrency
                );
    }

    private Long createAdminUserHandlingConcurrency() {
        try {
            return createAdminUser();
        } catch (UserRegistrationConflictException exception) {
            return userQuery
                    .findByCpf(adminBootstrapConfig.cpf())
                    .map(user -> user.id())
                    .orElseThrow(() -> exception);
        }
    }

    private Long createAdminUser() {
        Instant now = Instant.now(clock);

        return userRegistration.createWithPermanentCredential(
                new UserRegistrationData(
                        adminBootstrapConfig.name(), adminBootstrapConfig.birthDate(),
                        adminBootstrapConfig.email(), adminBootstrapConfig.cpf(),
                        adminBootstrapConfig.rg(), adminBootstrapConfig.phoneNumber(),
                        null, now
                ),
                adminBootstrapConfig.password()
        );
    }
}
