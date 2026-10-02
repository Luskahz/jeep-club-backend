package com.jeepclub.backend.memberships.core.application.service.membershipapplication;

import com.jeepclub.backend.memberships.core.application.service.membershipapplicantblock.AdminMembershipApplicantBlockService;
import com.jeepclub.backend.memberships.core.application.exception.MembershipApplicationNotFoundException;
import com.jeepclub.backend.memberships.core.application.exception.MembershipEmailRequiredException;
import com.jeepclub.backend.memberships.core.application.service.memberactivationtoken.MemberActivationTokenService;
import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import com.jeepclub.backend.memberships.core.port.CreateUserWithPendingFirstAccessPort;
import com.jeepclub.backend.memberships.core.port.MemberActivationMailSender;
import com.jeepclub.backend.memberships.core.port.PendingFirstAccessIdentity;
import com.jeepclub.backend.memberships.core.port.PendingFirstAccessUser;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @Mock
    private MemberActivationTokenService activationTokenService;

    private AdminMembershipApplicationService service;

    @BeforeEach
    void setUp() {
        service = new AdminMembershipApplicationService(
                applicationRepository,
                adminMembershipApplicantBlockService,
                createUserPort,
                mailSender,
                activationTokenService,
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
    void rejectionWithoutEmailStillSucceedsAndSkipsNotification() {
        MembershipApplication application = pendingApplication(null);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        service.reject(1L, 10L, "Not eligible");

        assertThat(application.getStatus()).isEqualTo(MembershipApplicationStatus.REJECTED);
        verifyNoInteractions(mailSender);
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
        PendingFirstAccessIdentity identity = new PendingFirstAccessIdentity(20L);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(createUserPort.createPendingUserForActivationLink(
                application.getName(),
                application.getEmail(),
                application.getCpf(),
                application.getPhoneNumber()
        )).thenReturn(identity);

        PendingFirstAccessIdentity result = service.approveWithAccessLink(1L, 10L);

        assertThat(result).isSameAs(identity);
        assertThat(application.getStatus()).isEqualTo(MembershipApplicationStatus.APPROVED);
        assertThat(application.getCreatedUserId()).isEqualTo(20L);
        verify(applicationRepository).save(application);
        verify(activationTokenService).issueAndSend(application);
    }

    @Test
    void accessLinkApprovalWithoutEmailFailsBeforeCreatingUser() {
        MembershipApplication application = pendingApplication(null);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.approveWithAccessLink(1L, 10L))
                .isInstanceOf(MembershipEmailRequiredException.class);

        verifyNoInteractions(createUserPort, activationTokenService);
        assertThat(application.getStatus()).isEqualTo(MembershipApplicationStatus.PENDING);
    }

    @Test
    void temporaryPasswordApprovalWorksWithoutEmail() {
        MembershipApplication application = pendingApplication(null);
        PendingFirstAccessUser pendingUser = new PendingFirstAccessUser(20L, "temporary-password");
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(createUserPort.createPendingUserWithTemporaryPassword(
                application.getName(), null, application.getCpf(), application.getPhoneNumber()
        )).thenReturn(pendingUser);

        assertThat(service.approveWithTemporaryPassword(1L, 10L)).isSameAs(pendingUser);
        assertThat(application.getStatus()).isEqualTo(MembershipApplicationStatus.APPROVED);
        assertThat(application.getCreatedUserId()).isEqualTo(20L);
    }

    @Test
    void resendUsesExistingUserAndDoesNotProvisionAnotherAccount() {
        MembershipApplication application = pendingApplication();
        application.approve(10L, 20L, NOW.minusSeconds(10));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        service.resendActivationLink(1L);

        verify(activationTokenService).issueAndSend(application);
        verifyNoInteractions(createUserPort);
    }

    @Test
    void findByIdThrowsNotFoundWhenApplicationDoesNotExist() {
        when(applicationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(MembershipApplicationNotFoundException.class)
                .hasMessageContaining("999");
    }

    private MembershipApplication pendingApplication() {
        return pendingApplication("candidate@example.com");
    }

    private MembershipApplication pendingApplication(String email) {
        return MembershipApplication.reconstitute(
                1L,
                "Candidate",
                CPF,
                email,
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
