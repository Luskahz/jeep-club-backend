package com.jeepclub.backend.memberships.core.application.service;

import com.jeepclub.backend.authentication.core.port.RefreshTokenGenerator;
import com.jeepclub.backend.authentication.core.port.RefreshTokenHashService;
import com.jeepclub.backend.memberships.core.application.exception.MembershipApplicationNotFoundException;
import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.domain.model.MemberActivationToken;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import com.jeepclub.backend.memberships.core.port.MemberActivationMailSender;
import com.jeepclub.backend.memberships.core.port.MembershipTimeProperties;
import com.jeepclub.backend.memberships.core.repository.MemberActivationTokenRepository;
import com.jeepclub.backend.memberships.core.repository.MembershipApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ResendActivationTokenService {

    private final MembershipApplicationRepository membershipApplicationRepository;
    private final MemberActivationTokenRepository memberActivationTokenRepository;
    private final RefreshTokenGenerator tokenGenerator;
    private final RefreshTokenHashService tokenHashService;
    private final MemberActivationMailSender mailSender;
    private final MembershipTimeProperties membershipTimeProperties;
    private final Clock clock;

    @Transactional
    public void resend(Long applicationId) {
        Instant now = Instant.now(clock);

        MembershipApplication application = membershipApplicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new MembershipApplicationNotFoundException(applicationId));

        MembershipApplicationStatus status = application.getStatus();

        if (status != MembershipApplicationStatus.INVITE_SENT
                && status != MembershipApplicationStatus.INVITE_EXPIRED) {
            throw new IllegalStateException(
                    "Reenvio de convite não permitido para solicitações com status: " + status.name()
            );
        }

        memberActivationTokenRepository.invalidateAllByApplicationId(applicationId);

        String rawToken = tokenGenerator.generate();
        String tokenHash = tokenHashService.hash(rawToken);

        MemberActivationToken activationToken = MemberActivationToken.create(
                applicationId,
                tokenHash,
                membershipTimeProperties.activationTokenTtl(),
                now
        );
        memberActivationTokenRepository.save(activationToken);

        application.markAsInviteSent(now);
        membershipApplicationRepository.save(application);

        mailSender.sendActivationLink(application.getEmail(), application.getName(), rawToken);
    }
}