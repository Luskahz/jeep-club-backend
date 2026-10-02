package com.jeepclub.backend.memberships.core.application.service.membershipapplication;

import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import com.jeepclub.backend.memberships.core.repository.MembershipApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipOnboardingCompletionServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-19T12:00:00Z");

    @Mock
    private MembershipApplicationRepository repository;

    @Test
    void completedTemporaryPasswordFirstAccessFinishesApprovedApplication() {
        MembershipApplication application = MembershipApplication.reconstitute(
                1L, "Candidate", "52998224725", null, "11999999999", null,
                MembershipApplicationStatus.APPROVED, null, 5L, 42L,
                NOW.minusSeconds(3600), NOW.minusSeconds(1800), null,
                NOW.minusSeconds(1800), 0L
        );
        when(repository.findByCreatedUserIdAndStatus(42L, MembershipApplicationStatus.APPROVED))
                .thenReturn(Optional.of(application));
        MembershipOnboardingCompletionService service =
                new MembershipOnboardingCompletionService(repository);

        service.completeApprovedApplicationForIdentity(42L, NOW);

        assertThat(application.getStatus()).isEqualTo(MembershipApplicationStatus.COMPLETED);
        assertThat(application.getFinishedAt()).isEqualTo(NOW);
        verify(repository).save(application);
    }
}
