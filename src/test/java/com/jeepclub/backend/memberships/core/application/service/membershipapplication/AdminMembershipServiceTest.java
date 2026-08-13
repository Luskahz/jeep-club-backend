package com.jeepclub.backend.memberships.core.application.service.membershipapplication;

import com.jeepclub.backend.memberships.core.application.service.membershipapplicantblock.MembershipApplicantBlockService;
import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import com.jeepclub.backend.memberships.core.port.CreateUserWithPendingFirstAccessPort;
import com.jeepclub.backend.memberships.core.port.MemberActivationMailSender;
import com.jeepclub.backend.memberships.core.repository.MembershipApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminMembershipServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-08T12:00:00Z");
    private static final String CPF = "52998224725";

    @Mock
    private MembershipApplicationRepository applicationRepository;
    @Mock
    private MembershipApplicantBlockService membershipApplicantBlockService;
    @Mock
    private CreateUserWithPendingFirstAccessPort createUserPort;
    @Mock
    private MemberActivationMailSender mailSender;

    private AdminMembershipService service;

    @BeforeEach
    void setUp() {
        service = new AdminMembershipService(
                applicationRepository,
                membershipApplicantBlockService,
                createUserPort,
                mailSender,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void normalRejectDoesNotBlockCpf() {
        MembershipApplication application = pendingApplication();
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        service.reject(1L, 10L, null);

        assertThat(application.getStatus()).isEqualTo(MembershipApplicationStatus.REJECTED);
        verify(applicationRepository).save(application);
        verifyNoInteractions(membershipApplicantBlockService);
    }

    @Test
    void rejectAndBlockUsesSameTimestampAndApplicationCpf() {
        MembershipApplication application = pendingApplication();
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        service.rejectAndBlock(1L, 10L, "Dados inconsistentes");

        assertThat(application.getStatus()).isEqualTo(MembershipApplicationStatus.REJECTED);
        assertThat(application.getReviewedAt()).isEqualTo(NOW);
        verify(applicationRepository).save(application);
        verify(membershipApplicantBlockService).block(
                CPF,
                "Dados inconsistentes",
                10L,
                NOW
        );
    }

    private MembershipApplication pendingApplication() {
        return MembershipApplication.reconstitute(
                1L,
                "Candidate",
                CPF,
                "candidate@example.com",
                "11999999999",
                null,
                MembershipApplicationStatus.PENDING,
                null,
                null,
                null,
                NOW.minusSeconds(3600),
                null,
                null,
                NOW.minusSeconds(3600),
                0L
        );
    }
}
