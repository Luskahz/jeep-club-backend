package com.jeepclub.backend.authentication.infra.integration.memberships;

import com.jeepclub.backend.authentication.core.application.result.PasswordResetLinkAdminResult;
import com.jeepclub.backend.authentication.core.application.service.account.AuthenticationAccountProvisioningService;
import com.jeepclub.backend.authentication.core.application.service.passwordrecovery.AdminPasswordRecoveryService;
import com.jeepclub.backend.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.authentication.core.port.RandomPasswordGenerator;
import com.jeepclub.backend.identity.api.module.IdentityRegistrationData;
import com.jeepclub.backend.memberships.core.port.CreateUserWithPendingFirstAccessPort;
import com.jeepclub.backend.memberships.core.port.PendingFirstAccessLink;
import com.jeepclub.backend.memberships.core.port.PendingFirstAccessUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class CreateUserWithPendingFirstAccessAdapter
        implements CreateUserWithPendingFirstAccessPort {

    private final AuthenticationAccountProvisioningService provisioningService;
    private final PasswordHasher passwordHasher;
    private final RandomPasswordGenerator passwordGenerator;
    private final AdminPasswordRecoveryService adminPasswordRecoveryService;
    private final Clock clock;

    @Override
    public PendingFirstAccessUser createPendingUserWithTemporaryPassword(
            String name,
            String email,
            String cpf,
            String phoneNumber
    ) {
        String temporaryPassword = passwordGenerator.generateSecurePassword();
        Long identityId = createPendingUser(
                name,
                email,
                cpf,
                phoneNumber,
                passwordHasher.hash(temporaryPassword)
        );

        return new PendingFirstAccessUser(identityId, temporaryPassword);
    }

    @Override
    public PendingFirstAccessLink createPendingUserWithAccessLink(
            String name,
            String email,
            String cpf,
            String phoneNumber
    ) {
        String internalPassword = passwordGenerator.generateSecurePassword();
        Long identityId = createPendingUser(
                name,
                email,
                cpf,
                phoneNumber,
                passwordHasher.hash(internalPassword)
        );

        PasswordResetLinkAdminResult resetLink =
                adminPasswordRecoveryService.generateResetLink(identityId);

        return new PendingFirstAccessLink(identityId, resetLink.resetLink());
    }

    private Long createPendingUser(
            String name,
            String email,
            String cpf,
            String phoneNumber,
            String passwordHash
    ) {
        Instant now = Instant.now(clock);
        return provisioningService.provisionPendingFirstAccess(
                new IdentityRegistrationData(
                        name, null, email, cpf, null, phoneNumber, null, now
                ),
                passwordHash
        );
    }
}
