package com.jeepclub.backend.authentication.infra.adapter;

import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.application.result.PasswordResetLinkAdminResult;
import com.jeepclub.backend.authentication.core.application.service.passwordrecovery.AdminPasswordRecoveryService;
import com.jeepclub.backend.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.authentication.core.port.RandomPasswordGenerator;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
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

    private final UserRepository userRepository;
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
        User savedUser = createPendingUser(
                name,
                email,
                cpf,
                phoneNumber,
                passwordHasher.hash(temporaryPassword)
        );

        return new PendingFirstAccessUser(savedUser.getId(), temporaryPassword);
    }

    @Override
    public PendingFirstAccessLink createPendingUserWithAccessLink(
            String name,
            String email,
            String cpf,
            String phoneNumber
    ) {
        String internalPassword = passwordGenerator.generateSecurePassword();
        User savedUser = createPendingUser(
                name,
                email,
                cpf,
                phoneNumber,
                passwordHasher.hash(internalPassword)
        );

        PasswordResetLinkAdminResult resetLink =
                adminPasswordRecoveryService.generateResetLink(savedUser.getId());

        return new PendingFirstAccessLink(savedUser.getId(), resetLink.resetLink());
    }

    private User createPendingUser(
            String name,
            String email,
            String cpf,
            String phoneNumber,
            String passwordHash
    ) {
        User newUser = User.createPendingFirstAccess(
                name,
                email,
                cpf,
                passwordHash,
                phoneNumber,
                Instant.now(clock)
        );

        return userRepository.create(newUser);
    }
}
