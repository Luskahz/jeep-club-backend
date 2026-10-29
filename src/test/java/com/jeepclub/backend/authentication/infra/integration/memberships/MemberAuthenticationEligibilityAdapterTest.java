package com.jeepclub.backend.authentication.infra.integration.memberships;

import com.jeepclub.backend.iam.authentication.core.domain.model.AuthenticationAccount;
import com.jeepclub.backend.iam.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.iam.authentication.infra.integration.memberships.MemberAuthenticationEligibilityAdapter;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MemberAuthenticationEligibilityAdapterTest {
    private final AuthenticationAccountRepository accounts=mock(AuthenticationAccountRepository.class);
    private final MemberAuthenticationEligibilityAdapter eligibility=new MemberAuthenticationEligibilityAdapter(accounts);
    private final Instant now=Instant.parse("2026-10-26T12:00:00Z");

    @Test void missingAuthenticationFailsClosedInPreviewAndLockedCommand() {
        when(accounts.findByIdentityId(1L)).thenReturn(Optional.empty());
        when(accounts.findByIdentityIdForUpdate(1L)).thenReturn(Optional.empty());
        assertThat(eligibility.hasCompletedFirstAccess(1L)).isFalse();
        assertThat(eligibility.lockAndHasCompletedFirstAccess(1L)).isFalse();
    }

    @Test void pendingAccountDoesNotQualifyEvenWithLoginOrPasswordMetadata() {
        var account=AuthenticationAccount.createPendingFirstAccess(1L,"synthetic-hash",now);
        account.recordSuccessfulLogin(now);
        when(accounts.findByIdentityId(1L)).thenReturn(Optional.of(account));
        when(accounts.findByIdentityIdForUpdate(1L)).thenReturn(Optional.of(account));
        assertThat(eligibility.hasCompletedFirstAccess(1L)).isFalse();
        assertThat(eligibility.lockAndHasCompletedFirstAccess(1L)).isFalse();
    }

    @Test void permanentProvisioningOutsideMembershipNeedsNoApplicationOrLogin() {
        var account=AuthenticationAccount.create(1L,"synthetic-hash",now);
        when(accounts.findByIdentityId(1L)).thenReturn(Optional.of(account));
        when(accounts.findByIdentityIdForUpdate(1L)).thenReturn(Optional.of(account));
        assertThat(eligibility.hasCompletedFirstAccess(1L)).isTrue();
        assertThat(eligibility.lockAndHasCompletedFirstAccess(1L)).isTrue();
        verify(accounts).findByIdentityIdForUpdate(1L);
    }

    @Test void temporaryRecoveryCannotTurnPendingFirstAccessIntoRegistryEligibility() {
        var account=AuthenticationAccount.createPendingFirstAccess(1L,"synthetic-hash",now);
        account.changeToTemporaryPassword("synthetic-temporary",now);
        when(accounts.findByIdentityId(1L)).thenReturn(Optional.of(account));
        when(accounts.findByIdentityIdForUpdate(1L)).thenReturn(Optional.of(account));
        assertThat(eligibility.hasCompletedFirstAccess(1L)).isFalse();
        assertThat(eligibility.lockAndHasCompletedFirstAccess(1L)).isFalse();
    }
}
