package com.jeepclub.backend.iam.authentication.infra.integration.memberships;

import com.jeepclub.backend.iam.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.iam.authentication.core.domain.model.AuthenticationAccount;
import com.jeepclub.backend.iam.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.memberships.core.port.MemberAuthenticationEligibility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MemberAuthenticationEligibilityAdapter implements MemberAuthenticationEligibility {
    private final AuthenticationAccountRepository accounts;

    @Override
    @Transactional(readOnly = true)
    public boolean hasCompletedFirstAccess(Long identityId) {
        return accounts.findByIdentityId(identityId).map(this::completed).orElse(false);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean lockAndHasCompletedFirstAccess(Long identityId) {
        // Auto-flush exposes activation saved earlier in this same onboarding transaction.
        // The repository uses a refreshed current locking read, including after an RR snapshot.
        return accounts.findByIdentityIdForUpdate(identityId).map(this::completed).orElse(false);
    }

    private boolean completed(AuthenticationAccount account) {
        // Permanent provisioning outside MembershipApplication is legitimate. CHANGE_REQUIRED
        // alone cannot prove completion: administrative recovery can also set it on a pending
        // account. Only the definitive credential qualifies a new Member; missing/other states fail closed.
        return account.getCredentialStatus() == CredentialStatus.PERMANENT;
    }
}
