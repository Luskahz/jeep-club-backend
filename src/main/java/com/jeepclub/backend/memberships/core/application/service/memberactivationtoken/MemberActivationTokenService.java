package com.jeepclub.backend.memberships.core.application.service.memberactivationtoken;

import com.jeepclub.backend.memberships.core.application.exception.MemberActivationTokenNotFoundException;
import com.jeepclub.backend.memberships.core.application.exception.MembershipApplicationNotFoundException;
import com.jeepclub.backend.memberships.core.application.exception.MembershipEmailRequiredException;
import com.jeepclub.backend.memberships.core.application.exception.MembershipFirstAccessConflictException;
import com.jeepclub.backend.memberships.core.domain.model.MemberActivationToken;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.port.ActivationRecipient;
import com.jeepclub.backend.memberships.core.port.ActivationRecipientPort;
import com.jeepclub.backend.memberships.core.port.CompletePendingFirstAccessPort;
import com.jeepclub.backend.memberships.core.port.MemberActivationMailSender;
import com.jeepclub.backend.memberships.core.port.MemberActivationTokenHashPort;
import com.jeepclub.backend.memberships.core.port.MemberActivationTokenGenerator;
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
public class MemberActivationTokenService {

    private final MemberActivationTokenRepository memberActivationTokenRepository;
    private final MemberActivationTokenHashPort tokenHashPort;
    private final MemberActivationTokenGenerator tokenGenerator;
    private final MembershipTimeProperties timeProperties;
    private final MembershipApplicationRepository applicationRepository;
    private final CompletePendingFirstAccessPort completeFirstAccessPort;
    private final ActivationRecipientPort activationRecipientPort;
    private final MemberActivationMailSender mailSender;
    private final Clock clock;

    @Transactional(readOnly = true)
    public void validate(String rawToken) {
        Instant now = Instant.now(clock);
        String tokenHash = tokenHashPort.hash(rawToken);

        MemberActivationToken token = memberActivationTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(MemberActivationTokenNotFoundException::new);

        token.validateOrThrow(now);
        requireApprovedApplication(token.getApplicationId());
    }

    @Transactional
    public void complete(String rawToken, String newPassword) {
        Instant now = Instant.now(clock);
        String tokenHash = tokenHashPort.hash(rawToken);
        MemberActivationToken token = memberActivationTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(MemberActivationTokenNotFoundException::new);
        token.validateOrThrow(now);

        MembershipApplication application = requireApprovedApplication(token.getApplicationId());
        if (application.getCreatedUserId() == null) {
            throw new MembershipFirstAccessConflictException("Approved membership application has no created User.");
        }

        completeFirstAccessPort.complete(application.getCreatedUserId(), newPassword);
        token.markAsUsed(now);
        application.complete(now);
        memberActivationTokenRepository.save(token);
        applicationRepository.save(application);
    }

    @Transactional
    public void issueAndSend(MembershipApplication application) {
        if (application.getStatus() != MembershipApplicationStatus.APPROVED
                || application.getCreatedUserId() == null) {
            throw new MembershipFirstAccessConflictException("Membership application is not ready for activation.");
        }
        ActivationRecipient recipient = activationRecipientPort
                .findByIdentityId(application.getCreatedUserId())
                .orElseThrow(() -> new MembershipFirstAccessConflictException("Created Identity User was not found."));
        if (recipient.email() == null || recipient.email().isBlank()) {
            throw new MembershipEmailRequiredException();
        }

        Instant now = Instant.now(clock);
        memberActivationTokenRepository.invalidateAllByApplicationId(application.getId(), now);
        String rawToken = tokenGenerator.generate();
        MemberActivationToken token = MemberActivationToken.create(
                application.getId(),
                tokenHashPort.hash(rawToken),
                timeProperties.activationTokenTtl(),
                now
        );
        memberActivationTokenRepository.save(token);
        mailSender.sendActivationLink(recipient.email(), recipient.name(), rawToken);
    }

    private MembershipApplication requireApprovedApplication(Long applicationId) {
        MembershipApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new MembershipApplicationNotFoundException(applicationId));
        if (application.getStatus() != MembershipApplicationStatus.APPROVED) {
            throw new MembershipFirstAccessConflictException("Membership application is not approved for activation.");
        }
        return application;
    }
}
