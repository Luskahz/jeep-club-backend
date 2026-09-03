package com.jeepclub.backend.authentication.core.application.service.bootstrap;

import com.jeepclub.backend.authentication.core.application.service.account.AuthenticationAccountProvisioningService;
import com.jeepclub.backend.authentication.core.application.exceptions.account.AuthenticationAccountConflictException;
import com.jeepclub.backend.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.identity.api.module.IdentityQuery;
import com.jeepclub.backend.identity.api.module.IdentityRegistrationData;
import com.jeepclub.backend.identity.core.application.exception.IdentityConflictException;
import com.jeepclub.backend.shared.bootstrap.AdminBootstrapConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DevelopmentAdminUserBootstrapService {

    private final IdentityQuery identityQuery;
    private final AuthenticationAccountProvisioningService provisioningService;
    private final PasswordHasher passwordHasher;
    private final AdminBootstrapConfig adminBootstrapConfig;
    private final Clock clock;

    public Long createAdminUserIfMissing() {
        return identityQuery
                .findByCpf(adminBootstrapConfig.cpf())
                .map(identity -> identity.id())
                .orElseGet(
                        this::createAdminUserHandlingConcurrency
                );
    }

    private Long createAdminUserHandlingConcurrency() {
        try {
            return createAdminUser();
        } catch (IdentityConflictException | AuthenticationAccountConflictException exception) {
            return identityQuery
                    .findByCpf(adminBootstrapConfig.cpf())
                    .map(identity -> identity.id())
                    .orElseThrow(() -> exception);
        }
    }

    private Long createAdminUser() {
        Instant now = Instant.now(clock);

        String passwordHash =
                passwordHasher.hash(
                        adminBootstrapConfig.password()
                );

        return provisioningService.provision(
                new IdentityRegistrationData(
                        adminBootstrapConfig.name(), adminBootstrapConfig.birthDate(),
                        adminBootstrapConfig.email(), adminBootstrapConfig.cpf(),
                        adminBootstrapConfig.rg(), adminBootstrapConfig.phoneNumber(),
                        null, now
                ),
                passwordHash
        );
    }
}
