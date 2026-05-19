package com.jeepclub.backend.membership.core.application.service;

import com.jeepclub.backend.authentication.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.authentication.core.domain.model.MembershipApplication;
import com.jeepclub.backend.authentication.core.port.RefreshTokenGenerator;
import com.jeepclub.backend.authentication.core.port.RefreshTokenHashService;
import com.jeepclub.backend.authentication.core.repository.MembershipApplicationRepository;
import com.jeepclub.backend.membership.core.application.exception.MembershipApplicationNotFoundException;
import com.jeepclub.backend.membership.core.domain.model.MemberActivationToken;
import com.jeepclub.backend.membership.core.port.MemberActivationMailSender;
import com.jeepclub.backend.membership.core.repository.MemberActivationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ResendActivationTokenService {

    private static final Duration ACTIVATION_TOKEN_TTL = Duration.ofHours(72);

    private final MembershipApplicationRepository membershipApplicationRepository;
    private final MemberActivationTokenRepository memberActivationTokenRepository;
    private final RefreshTokenGenerator tokenGenerator;
    private final RefreshTokenHashService tokenHashService;
    private final MemberActivationMailSender mailSender;

    @Transactional
    public void resend(Long applicationId) {
        Instant now = Instant.now();

        MembershipApplication application = membershipApplicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new MembershipApplicationNotFoundException(applicationId));

        MembershipApplicationStatus status = application.getStatus();

        // Só permite reenvio se estiver INVITE_SENT ou INVITE_EXPIRED
        if (status != MembershipApplicationStatus.INVITE_SENT
                && status != MembershipApplicationStatus.INVITE_EXPIRED) {
            throw new IllegalStateException(
                    "Reenvio de convite não permitido para solicitações com status: " + status.name()
            );
        }

        // Invalida todos os tokens anteriores e gera novo
        memberActivationTokenRepository.invalidateAllByApplicationId(applicationId);

        String rawToken = tokenGenerator.generate();
        String tokenHash = tokenHashService.hash(rawToken);

        MemberActivationToken activationToken = MemberActivationToken.create(
                applicationId,
                tokenHash,
                ACTIVATION_TOKEN_TTL,
                now
        );
        memberActivationTokenRepository.save(activationToken);

        // Garante que o status volta para INVITE_SENT
        application.markAsInviteSent(now);
        membershipApplicationRepository.save(application);

        mailSender.sendActivationLink(application.getEmail(), application.getName(), rawToken);
    }
}