package com.jeepclub.backend.memberships.core.application.service.membershipapplication;

import com.jeepclub.backend.memberships.core.application.exception.MembershipApplicationAlreadyProcessedException;
import com.jeepclub.backend.memberships.core.application.exception.MembershipApplicationNotFoundException;
import com.jeepclub.backend.memberships.core.application.exception.MembershipEmailRequiredException;
import com.jeepclub.backend.memberships.core.application.service.memberactivationtoken.MemberActivationTokenService;
import com.jeepclub.backend.memberships.core.application.service.membershipapplicantblock.AdminMembershipApplicantBlockService;
import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import com.jeepclub.backend.memberships.core.port.CreateUserWithPendingFirstAccessPort;
import com.jeepclub.backend.memberships.core.port.*;
import com.jeepclub.backend.memberships.core.port.PendingFirstAccessIdentity;
import com.jeepclub.backend.memberships.core.port.PendingFirstAccessUser;
import com.jeepclub.backend.memberships.core.repository.MembershipApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMembershipApplicationServiceTest {

    @Test
    void rejectionNotificationUsesPersistedRecipientAndTrimmedReason() {
        MembershipApplication application = pendingApplication();
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.saveAndFlush(application)).thenReturn(application);
        service.reject(1L, 10L, "  Not eligible  ");
        assertThat(application.getRejectionReason()).isEqualTo("Not eligible");
        verify(mailSender).enqueue(application);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @org.junit.jupiter.params.provider.ValueSource(strings = {"   "})
    void rejectionWithAbsentReasonStillQueuesGenericNotice(String reason) {
        MembershipApplication application = pendingApplication();
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.saveAndFlush(application)).thenReturn(application);
        service.reject(1L, 10L, reason);
        verify(mailSender).enqueue(application);
    }

    @Test
    void findByIdReturnsExistingApplicationAndMissingApprovalOrResendDoesNotProvision() {
        MembershipApplication application = pendingApplication();
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        assertThat(service.findById(1L)).isSameAs(application);
        assertThatThrownBy(() -> service.approveWithAccessLink(999L, 10L))
                .isInstanceOf(MembershipApplicationNotFoundException.class);
        assertThatThrownBy(() -> service.resendActivationLink(999L))
                .isInstanceOf(MembershipApplicationNotFoundException.class);
        verifyNoInteractions(createUserPort, activationTokenService);
    }

    private static final Instant NOW = Instant.parse("2026-08-08T12:00:00Z");
    private static final String CPF = "52998224725";

    @Mock
    private MembershipApplicationRepository applicationRepository;
    @Mock
    private AdminMembershipApplicantBlockService adminMembershipApplicantBlockService;
    @Mock
    private CreateUserWithPendingFirstAccessPort createUserPort;
    @Mock
    private MembershipRejectionNotifier mailSender;
    @Mock private PendingMembershipIdentityReversal identityReversal;
    @Mock private com.jeepclub.backend.memberships.core.repository.MemberActivationTokenRepository activationTokens;
    @Mock private MembershipReviewHistory history;
    @Mock private PendingMembershipAuthentication pendingAuthentication;
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
                Clock.fixed(NOW, ZoneOffset.UTC), identityReversal, activationTokens, history, pendingAuthentication
        );
    }

    @Test
    void normalRejectDoesNotBlockCpf() {
        MembershipApplication application = pendingApplication();
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(application));

        service.reject(1L, 10L, null);

        assertThat(application.getStatus()).isEqualTo(MembershipApplicationStatus.REJECTED);
        verify(applicationRepository).saveAndFlush(application);
        verifyNoInteractions(adminMembershipApplicantBlockService);
    }

    @Test
    void rejectionWithoutEmailStillSucceedsAndSkipsNotification() {
        MembershipApplication application = pendingApplication(null);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(application));

        service.reject(1L, 10L, "Not eligible");

        assertThat(application.getStatus()).isEqualTo(MembershipApplicationStatus.REJECTED);
        verifyNoInteractions(mailSender);
    }

    @Test
    void rejectAndBlockUsesSameTimestampAndApplicationCpf() {
        MembershipApplication application = pendingApplication();
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(application));

        service.rejectAndBlock(1L, 10L, "Dados inconsistentes");

        assertThat(application.getStatus()).isEqualTo(MembershipApplicationStatus.REJECTED);
        assertThat(application.getReviewedAt()).isEqualTo(NOW);
        verify(applicationRepository).saveAndFlush(application);
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
        when(applicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.saveAndFlush(application)).thenReturn(application);
        PendingFirstAccessIdentity identity = new PendingFirstAccessIdentity(20L);
        when(createUserPort.createPendingUserForActivationLink(
                application.getName(),
                application.getEmail(),
                application.getCpf(),
                application.getPhoneNumber()
        )).thenReturn(identity);

        when(activationTokenService.issueAndSend(application)).thenReturn("https://app.example/activate?token=test-bearer");
        AccessLinkApproval result = service.approveWithAccessLink(1L, 10L);

        assertThat(result.userId()).isEqualTo(identity.userId());
        assertThat(result.activationLink()).isEqualTo("https://app.example/activate?token=test-bearer");
        assertThat(application.getStatus()).isEqualTo(MembershipApplicationStatus.APPROVED);
        assertThat(application.getCreatedUserId()).isEqualTo(20L);
        verify(applicationRepository).saveAndFlush(application);
        verify(activationTokenService).issueAndSend(application);
    }

    @Test
    void accessLinkApprovalWithoutEmailFailsBeforeCreatingUser() {
        MembershipApplication application = pendingApplication(null);
        when(applicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.approveWithAccessLink(1L, 10L))
                .isInstanceOf(MembershipEmailRequiredException.class);

        verifyNoInteractions(createUserPort, activationTokenService);
        assertThat(application.getStatus()).isEqualTo(MembershipApplicationStatus.PENDING);
    }

    @Test
    void temporaryPasswordApprovalWorksWithoutEmail() {
        MembershipApplication application = pendingApplication(null);
        when(applicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.saveAndFlush(application)).thenReturn(application);
        PendingFirstAccessUser pendingUser = new PendingFirstAccessUser(20L, "temporary-password");
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
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(application));
        application.approve(10L, 20L, NOW.minusSeconds(10));

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

    @Test
    void listAllDelegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<MembershipApplication> page = new PageImpl<>(List.of(pendingApplication()));
        when(applicationRepository.findAll(pageable)).thenReturn(page);

        Page<MembershipApplication> result = service.listAll(pageable);

        assertThat(result).isSameAs(page);
        verify(applicationRepository).findAll(pageable);
    }

    @Test
    void listByStatusDelegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<MembershipApplication> page = new PageImpl<>(List.of(pendingApplication()));
        when(applicationRepository.findAllByStatus(MembershipApplicationStatus.PENDING, pageable))
                .thenReturn(page);

        Page<MembershipApplication> result = service.listByStatus(MembershipApplicationStatus.PENDING, pageable);

        assertThat(result).isSameAs(page);
        verify(applicationRepository).findAllByStatus(MembershipApplicationStatus.PENDING, pageable);
    }

    @Test
    void operationsOnNonPendingApplicationThrowAlreadyProcessedException() {
        MembershipApplication approvedApp = pendingApplication();
        approvedApp.approve(10L, 20L, NOW);
        approvedApp.complete(NOW);
        when(applicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(approvedApp));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(approvedApp));

        assertThatThrownBy(() -> service.approveWithTemporaryPassword(1L, 10L))
                .isInstanceOf(MembershipApplicationAlreadyProcessedException.class);

        assertThatThrownBy(() -> service.approveWithAccessLink(1L, 10L))
                .isInstanceOf(MembershipApplicationAlreadyProcessedException.class);

        assertThatThrownBy(() -> service.reject(1L, 10L, "reason"))
                .isInstanceOf(MembershipApplicationAlreadyProcessedException.class);

        assertThatThrownBy(() -> service.rejectAndBlock(1L, 10L, "reason"))
                .isInstanceOf(MembershipApplicationAlreadyProcessedException.class);
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
