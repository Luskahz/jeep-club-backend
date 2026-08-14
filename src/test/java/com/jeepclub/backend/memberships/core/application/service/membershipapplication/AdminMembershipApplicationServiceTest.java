package com.jeepclub.backend.memberships.core.application.service.membershipapplication;

import com.jeepclub.backend.memberships.core.application.service.membershipapplicantblock.AdminMembershipApplicantBlockService;
import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import com.jeepclub.backend.memberships.core.port.CreateUserWithPendingFirstAccessPort;
import com.jeepclub.backend.memberships.core.port.MemberActivationMailSender;
import com.jeepclub.backend.memberships.core.port.PendingFirstAccessLink;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMembershipApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-08T12:00:00Z");
    private static final String CPF = "52998224725";

    @Mock
    private MembershipApplicationRepository applicationRepository;
    @Mock
    private AdminMembershipApplicantBlockService adminMembershipApplicantBlockService;
    @Mock
    private CreateUserWithPendingFirstAccessPort createUserPort;
    @Mock
    private MemberActivationMailSender mailSender;

    private AdminMembershipApplicationService service;

    @BeforeEach
    void setUp() {
        service = new AdminMembershipApplicationService(
                applicationRepository,
                adminMembershipApplicantBlockService,
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
        verifyNoInteractions(adminMembershipApplicantBlockService);
    }

    @Test
    void rejectAndBlockUsesSameTimestampAndApplicationCpf() {
        MembershipApplication application = pendingApplication();
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        service.rejectAndBlock(1L, 10L, "Dados inconsistentes");

        assertThat(application.getStatus()).isEqualTo(MembershipApplicationStatus.REJECTED);
        assertThat(application.getReviewedAt()).isEqualTo(NOW);
        verify(applicationRepository).save(application);
        verify(adminMembershipApplicantBlockService).block(
                CPF,
                "Dados inconsistentes",
                10L,
                NOW
        );
    }

    @Test
    void approveWithAccessLinkBelongsToMembershipApplicationService() {
        MembershipApplication application = pendingApplication();
        PendingFirstAccessLink link = new PendingFirstAccessLink(20L, "https://example.test/access");
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(createUserPort.createPendingUserWithAccessLink(
                application.getName(),
                application.getEmail(),
                application.getCpf(),
                application.getPhoneNumber()
        )).thenReturn(link);

        PendingFirstAccessLink result = service.approveWithAccessLink(1L, 10L);

        assertThat(result).isSameAs(link);
        assertThat(application.getStatus()).isEqualTo(MembershipApplicationStatus.APPROVED);
        assertThat(application.getCreatedUserId()).isEqualTo(20L);
        verify(applicationRepository).save(application);
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
