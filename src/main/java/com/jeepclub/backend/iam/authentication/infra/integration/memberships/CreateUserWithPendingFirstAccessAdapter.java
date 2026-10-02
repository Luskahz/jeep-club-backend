package com.jeepclub.backend.iam.authentication.infra.integration.memberships;

import com.jeepclub.backend.iam.authentication.core.port.RandomPasswordGenerator;
import com.jeepclub.backend.iam.identity.api.module.UserRegistration;
import com.jeepclub.backend.iam.identity.api.module.UserRegistrationData;
import com.jeepclub.backend.memberships.core.port.CreateUserWithPendingFirstAccessPort;
import com.jeepclub.backend.memberships.core.port.PendingFirstAccessIdentity;
import com.jeepclub.backend.memberships.core.port.PendingFirstAccessUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class CreateUserWithPendingFirstAccessAdapter
        implements CreateUserWithPendingFirstAccessPort {

    private final UserRegistration userRegistration;
    private final RandomPasswordGenerator passwordGenerator;
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
                temporaryPassword
        );

        return new PendingFirstAccessUser(identityId, temporaryPassword);
    }

    @Override
    public PendingFirstAccessIdentity createPendingUserForActivationLink(
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
                internalPassword
        );

        return new PendingFirstAccessIdentity(identityId);
    }

    private Long createPendingUser(
            String name,
            String email,
            String cpf,
            String phoneNumber,
            String rawPassword
    ) {
        Instant now = Instant.now(clock);
        return userRegistration.createPendingFirstAccess(
                new UserRegistrationData(
                        name, null, email, cpf, null, phoneNumber, null, now
                ),
                rawPassword
        );
    }
}
