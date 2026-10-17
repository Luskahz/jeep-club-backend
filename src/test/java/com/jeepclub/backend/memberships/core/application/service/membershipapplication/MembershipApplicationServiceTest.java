package com.jeepclub.backend.memberships.core.application.service.membershipapplication;

import com.jeepclub.backend.memberships.core.application.exception.MembershipApplicantBlockedException;
import com.jeepclub.backend.memberships.core.application.exception.MembershipCpfAlreadyRegisteredException;
import com.jeepclub.backend.memberships.core.application.exception.MembershipEmailAlreadyInUseException;
import com.jeepclub.backend.memberships.core.application.exception.MembershipEmailAlreadyRegisteredException;
import com.jeepclub.backend.memberships.core.application.result.EnsureMembershipRequestResult;
import com.jeepclub.backend.memberships.core.application.service.membershipapplicantblock.MembershipApplicantBlockService;
import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import com.jeepclub.backend.memberships.core.port.UserExistencePort;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipApplicationServiceTest {

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @org.junit.jupiter.params.provider.ValueSource(strings = {"   ", "  Hello club  "})
    void creationNormalizesOptionalMessageWithoutLosingNonBlankText(String message) {
        when(applicationRepository.save(any(MembershipApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.ensure("Candidate", CPF, null, "(11) 99999-9999", message);
        assertThat(result.application().getMessage())
                .isEqualTo(message == null || message.isBlank() ? null : "Hello club");
        assertThat(result.application().getPhoneNumber()).isEqualTo("11999999999");
    }

    private static final Instant NOW = Instant.parse("2026-08-08T12:00:00Z");
    private static final String CPF = "52998224725";
    private static final String FORMATTED_CPF = "529.982.247-25";

    @Mock
    private MembershipApplicationRepository applicationRepository;
    @Mock
    private MembershipApplicantBlockService membershipApplicantBlockService;
    @Mock
    private UserExistencePort userExistencePort;

    private MembershipApplicationService service;

    @BeforeEach
    void setUp() {
        service = new MembershipApplicationService(
                applicationRepository,
                membershipApplicantBlockService,
                userExistencePort,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void activeBlockIsCheckedBeforePendingApplicationAndDoesNotExposeReason() {
        when(membershipApplicantBlockService.isBlocked(CPF)).thenReturn(true);

        assertThatThrownBy(() -> ensure(FORMATTED_CPF, "candidate@example.com"))
                .isInstanceOf(MembershipApplicantBlockedException.class)
                .hasMessage("Não é possível realizar uma nova solicitação de associação.")
                .hasMessageNotContaining("fraude");

        verify(membershipApplicantBlockService).isBlocked(CPF);
        verifyNoInteractions(applicationRepository, userExistencePort);
    }

    @Test
    void repeatedPendingApplicationReturnsExistingApplication() {
        MembershipApplication pending = application(1L, MembershipApplicationStatus.PENDING);
        when(applicationRepository.findByCpfAndStatus(CPF, MembershipApplicationStatus.PENDING))
                .thenReturn(Optional.of(pending));

        EnsureMembershipRequestResult result = ensure(FORMATTED_CPF, "candidate@example.com");

        assertThat(result.created()).isFalse();
        assertThat(result.application()).isSameAs(pending);
        verify(applicationRepository, never()).save(any());
        verifyNoInteractions(userExistencePort);
    }

    @Test
    void rejectedApplicationAllowsNewApplication() {
        when(applicationRepository.findByCpfAndStatus(CPF, MembershipApplicationStatus.PENDING))
                .thenReturn(Optional.empty());
        when(applicationRepository.save(any(MembershipApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EnsureMembershipRequestResult result = ensure(FORMATTED_CPF, "Candidate@Example.com");

        assertThat(result.created()).isTrue();
        assertThat(result.application().getStatus()).isEqualTo(MembershipApplicationStatus.PENDING);
        assertThat(result.application().getCpf()).isEqualTo(CPF);
        assertThat(result.application().getEmail()).isEqualTo("candidate@example.com");
    }

    @Test
    void applicationWithoutEmailSkipsAllEmailConflictQueries() {
        when(applicationRepository.findByCpfAndStatus(CPF, MembershipApplicationStatus.PENDING))
                .thenReturn(Optional.empty());
        when(applicationRepository.save(any(MembershipApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EnsureMembershipRequestResult result = ensure(FORMATTED_CPF, "   ");

        assertThat(result.application().getEmail()).isNull();
        verify(applicationRepository, never()).existsByEmailAndStatus(any(), any());
        verify(userExistencePort, never()).existsByEmail(any());
    }

    @Test
    void existingUserWithSameCpfThrowsMembershipCpfAlreadyRegisteredException() {
        when(applicationRepository.findByCpfAndStatus(CPF, MembershipApplicationStatus.PENDING))
                .thenReturn(Optional.empty());
        when(userExistencePort.existsByCpf(CPF)).thenReturn(true);

        assertThatThrownBy(() -> ensure(FORMATTED_CPF, "candidate@example.com"))
                .isInstanceOf(MembershipCpfAlreadyRegisteredException.class)
                .hasMessageContaining(CPF);

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void existingPendingApplicationWithSameEmailThrowsMembershipEmailAlreadyInUseException() {
        when(applicationRepository.findByCpfAndStatus(CPF, MembershipApplicationStatus.PENDING))
                .thenReturn(Optional.empty());
        when(userExistencePort.existsByCpf(CPF)).thenReturn(false);
        when(applicationRepository.existsByEmailAndStatus("candidate@example.com", MembershipApplicationStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> ensure(FORMATTED_CPF, "candidate@example.com"))
                .isInstanceOf(MembershipEmailAlreadyInUseException.class)
                .hasMessageContaining("candidate@example.com");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void existingUserWithSameEmailThrowsMembershipEmailAlreadyRegisteredException() {
        when(applicationRepository.findByCpfAndStatus(CPF, MembershipApplicationStatus.PENDING))
                .thenReturn(Optional.empty());
        when(userExistencePort.existsByCpf(CPF)).thenReturn(false);
        when(applicationRepository.existsByEmailAndStatus("candidate@example.com", MembershipApplicationStatus.PENDING))
                .thenReturn(false);
        when(userExistencePort.existsByEmail("candidate@example.com")).thenReturn(true);

        assertThatThrownBy(() -> ensure(FORMATTED_CPF, "candidate@example.com"))
                .isInstanceOf(MembershipEmailAlreadyRegisteredException.class)
                .hasMessageContaining("candidate@example.com");

        verify(applicationRepository, never()).save(any());
    }

    private EnsureMembershipRequestResult ensure(String cpf, String email) {
        return service.ensure(
                "Candidate",
                cpf,
                email,
                "(11) 99999-9999",
                null
        );
    }

    private MembershipApplication application(Long id, MembershipApplicationStatus status) {
        return MembershipApplication.reconstitute(
                id,
                "Candidate",
                CPF,
                "candidate@example.com",
                "11999999999",
                null,
                status,
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
