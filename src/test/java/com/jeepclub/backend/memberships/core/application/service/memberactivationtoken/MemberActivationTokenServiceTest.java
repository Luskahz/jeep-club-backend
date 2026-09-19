package com.jeepclub.backend.memberships.core.application.service.memberactivationtoken;

import com.jeepclub.backend.memberships.core.application.exception.MemberActivationTokenAlreadyUsedException;
import com.jeepclub.backend.memberships.core.application.exception.MemberActivationTokenExpiredException;
import com.jeepclub.backend.memberships.core.application.exception.MemberActivationTokenNotFoundException;
import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.domain.model.MemberActivationToken;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import com.jeepclub.backend.memberships.core.port.*;
import com.jeepclub.backend.memberships.core.repository.MemberActivationTokenRepository;
import com.jeepclub.backend.memberships.core.repository.MembershipApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberActivationTokenServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-19T12:00:00Z");
    @Mock MemberActivationTokenRepository tokenRepository;
    @Mock MemberActivationTokenHashPort hashPort;
    @Mock MemberActivationTokenGenerator tokenGenerator;
    @Mock MembershipTimeProperties timeProperties;
    @Mock MembershipApplicationRepository applicationRepository;
    @Mock CompletePendingFirstAccessPort firstAccessPort;
    @Mock ActivationRecipientPort recipientPort;
    @Mock MemberActivationMailSender mailSender;
    private MemberActivationTokenService service;

    @BeforeEach
    void setUp() {
        service = new MemberActivationTokenService(
                tokenRepository, hashPort, tokenGenerator, timeProperties,
                applicationRepository, firstAccessPort, recipientPort, mailSender,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void repeatedValidationIsReadOnly() {
        MemberActivationToken token = validToken();
        when(hashPort.hash("raw")).thenReturn("hash");
        when(tokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(approvedApplication()));

        service.validate("raw");
        service.validate("raw");

        assertThat(token.getUsedAt()).isNull();
        verify(tokenRepository, never()).save(any());
        verify(firstAccessPort, never()).complete(any(), any());
    }

    @Test
    void validationRejectsUnknownTokenWithoutWritingState() {
        when(hashPort.hash("missing")).thenReturn("missing-hash");
        when(tokenRepository.findByTokenHash("missing-hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validate("missing"))
                .isInstanceOf(MemberActivationTokenNotFoundException.class);

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(applicationRepository, firstAccessPort);
    }

    @Test
    void validationRejectsExpiredTokenWithoutWritingState() {
        MemberActivationToken token = MemberActivationToken.reconstitute(
                10L, 1L, "hash", NOW.minusSeconds(1), null, NOW.minusSeconds(3600)
        );
        when(hashPort.hash("expired")).thenReturn("hash");
        when(tokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.validate("expired"))
                .isInstanceOf(MemberActivationTokenExpiredException.class);

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(applicationRepository, firstAccessPort);
    }

    @Test
    void validationRejectsUsedTokenWithoutWritingState() {
        MemberActivationToken token = MemberActivationToken.reconstitute(
                10L, 1L, "hash", NOW.plusSeconds(3600), NOW.minusSeconds(1), NOW.minusSeconds(60)
        );
        when(hashPort.hash("used")).thenReturn("hash");
        when(tokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.validate("used"))
                .isInstanceOf(MemberActivationTokenAlreadyUsedException.class);

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(applicationRepository, firstAccessPort);
    }

    @Test
    void completionConsumesTokenAndApplicationOnlyAfterAuthenticationSucceeds() {
        MemberActivationToken token = validToken();
        MembershipApplication application = approvedApplication();
        when(hashPort.hash("raw")).thenReturn("hash");
        when(tokenRepository.findByTokenHashForUpdate("hash")).thenReturn(Optional.of(token));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        service.complete("raw", "new-password");

        assertThat(token.getUsedAt()).isEqualTo(NOW);
        assertThat(application.getStatus()).isEqualTo(MembershipApplicationStatus.COMPLETED);
        InOrder order = inOrder(firstAccessPort, tokenRepository, applicationRepository);
        order.verify(firstAccessPort).complete(20L, "new-password");
        order.verify(tokenRepository).save(token);
        order.verify(applicationRepository).save(application);
    }

    @Test
    void authenticationFailureDoesNotConsumeAnything() {
        MemberActivationToken token = validToken();
        MembershipApplication application = approvedApplication();
        when(hashPort.hash("raw")).thenReturn("hash");
        when(tokenRepository.findByTokenHashForUpdate("hash")).thenReturn(Optional.of(token));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        doThrow(new IllegalStateException("authentication failed"))
                .when(firstAccessPort).complete(20L, "new-password");

        assertThatThrownBy(() -> service.complete("raw", "new-password"))
                .hasMessage("authentication failed");
        assertThat(token.getUsedAt()).isNull();
        assertThat(application.getStatus()).isEqualTo(MembershipApplicationStatus.APPROVED);
        verify(tokenRepository, never()).save(any());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void issuancePersistsOnlyHashAndInvalidatesPreviousTokens() {
        MembershipApplication application = approvedApplication();
        when(recipientPort.findByIdentityId(20L))
                .thenReturn(Optional.of(new ActivationRecipient("Candidate", "candidate@example.com")));
        when(tokenGenerator.generate()).thenReturn("raw-secret");
        when(hashPort.hash("raw-secret")).thenReturn("stored-hash");
        when(timeProperties.activationTokenTtl()).thenReturn(Duration.ofHours(72));

        service.issueAndSend(application);

        ArgumentCaptor<MemberActivationToken> captor = ArgumentCaptor.forClass(MemberActivationToken.class);
        verify(tokenRepository).invalidateAllByApplicationId(1L, NOW);
        verify(tokenRepository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isEqualTo("stored-hash");
        assertThat(captor.getValue().getTokenHash()).doesNotContain("raw-secret");
        verify(mailSender).sendActivationLink("candidate@example.com", "Candidate", "raw-secret");
    }

    private MemberActivationToken validToken() {
        return MemberActivationToken.reconstitute(
                10L, 1L, "hash", NOW.plusSeconds(3600), null, NOW.minusSeconds(60)
        );
    }

    private MembershipApplication approvedApplication() {
        return MembershipApplication.reconstitute(
                1L, "Candidate", "52998224725", "candidate@example.com", "11999999999", null,
                MembershipApplicationStatus.APPROVED, null, 5L, 20L,
                NOW.minusSeconds(3600), NOW.minusSeconds(1800), null,
                NOW.minusSeconds(1800), 0L
        );
    }
}
