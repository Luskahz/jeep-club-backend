package com.jeepclub.backend.iam.authentication.infra.integration.memberships;

import com.jeepclub.backend.iam.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.memberships.core.application.exception.MembershipFirstAccessConflictException;
import com.jeepclub.backend.memberships.core.port.PendingMembershipAuthentication;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PendingMembershipAuthenticationAdapter implements PendingMembershipAuthentication {
    private final AuthenticationAccountRepository accounts;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void requirePending(Long identityId) {
        var account = accounts.findByIdentityIdForUpdate(identityId)
                .orElseThrow(() -> new MembershipFirstAccessConflictException("Provisioned authentication is unavailable."));
        if (!account.isPendingFirstAccess() || account.getPasswordChangedAt() != null || account.getLastLoginAt() != null) {
            throw new MembershipFirstAccessConflictException("First access is no longer pending.");
        }
    }
}
