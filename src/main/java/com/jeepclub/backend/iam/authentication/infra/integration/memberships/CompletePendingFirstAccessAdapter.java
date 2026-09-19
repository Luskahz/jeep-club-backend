package com.jeepclub.backend.iam.authentication.infra.integration.memberships;

import com.jeepclub.backend.iam.authentication.core.application.service.internal.CredentialRevocationService;
import com.jeepclub.backend.iam.authentication.core.domain.model.AuthenticationAccount;
import com.jeepclub.backend.iam.authentication.core.domain.exception.account.AuthenticationAccountBlockedException;
import com.jeepclub.backend.iam.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.iam.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.memberships.core.application.exception.MembershipFirstAccessConflictException;
import com.jeepclub.backend.memberships.core.port.CompletePendingFirstAccessPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class CompletePendingFirstAccessAdapter implements CompletePendingFirstAccessPort {

    private final AuthenticationAccountRepository accountRepository;
    private final PasswordHasher passwordHasher;
    private final CredentialRevocationService credentialRevocationService;
    private final Clock clock;

    @Override
    public void complete(Long identityId, String newPassword) {
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 72) {
            throw new IllegalArgumentException("newPassword must contain between 8 and 72 characters");
        }
        Instant now = Instant.now(clock);
        AuthenticationAccount account = accountRepository.findByIdentityIdForUpdate(identityId)
                .orElseThrow(() -> new MembershipFirstAccessConflictException(
                        "Authentication account for first access was not found."
                ));
        try {
            account.assertCanAttemptLogin();
        } catch (AuthenticationAccountBlockedException exception) {
            throw new MembershipFirstAccessConflictException(
                    "Authentication account cannot complete first access in its current state."
            );
        }
        if (!account.isPendingFirstAccess()) {
            throw new MembershipFirstAccessConflictException(
                    "Authentication account is not pending first access."
            );
        }

        account.changePassword(passwordHasher.hash(newPassword), now);
        credentialRevocationService.revokeAllForUser(identityId, now);
        accountRepository.save(account);
    }
}
