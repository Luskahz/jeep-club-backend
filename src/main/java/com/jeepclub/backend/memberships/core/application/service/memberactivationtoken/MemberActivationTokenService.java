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
import com.jeepclub.backend.memberships.core.port.MemberActivationInvitationDelivery;
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
    private final MemberActivationInvitationDelivery invitationDelivery;
    private final com.jeepclub.backend.memberships.core.port.MemberActivationLinkFactory linkFactory;
    private final com.jeepclub.backend.memberships.core.port.PendingMembershipAuthentication pendingAuthentication;
    private final Clock clock;
    private final com.jeepclub.backend.memberships.core.application.service.member.AdminMemberService memberRegistry;

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
        String tokenHash = tokenHashPort.hash(rawToken);
        // Resolve without locking the token, then establish the global Identity -> token/account order.
        Long applicationId = memberActivationTokenRepository.findApplicationIdByTokenHash(tokenHash)
                .orElseThrow(MemberActivationTokenNotFoundException::new);
        MembershipApplication candidateApplication = requireApprovedApplication(applicationId);
        if (candidateApplication.getCreatedUserId() == null) {
            throw new MembershipFirstAccessConflictException("Approved membership application has no created User.");
        }
        activationRecipientPort.findByIdentityId(candidateApplication.getCreatedUserId())
                .orElseThrow(() -> new MembershipFirstAccessConflictException("Created Identity User was not found."));
        MembershipApplication application = requireApprovedApplicationForUpdate(applicationId);
        if (!java.util.Objects.equals(application.getCreatedUserId(), candidateApplication.getCreatedUserId())) {
            throw new MembershipFirstAccessConflictException("Membership provisioning changed concurrently.");
        }
        MemberActivationToken token = memberActivationTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(MemberActivationTokenNotFoundException::new);
        Instant now = Instant.now(clock);
        token.validateOrThrow(now);

        completeFirstAccessPort.complete(application.getCreatedUserId(), newPassword);
        // Authentication may wait for its account lock. Expiry here must roll back its changes too.
        now = Instant.now(clock);
        token.validateOrThrow(now);
        token.markAsUsed(now);
        application.complete(now);
        memberActivationTokenRepository.save(token);
        applicationRepository.save(application);
        memberRegistry.issueOnCompletion(application.getCreatedUserId());
    }

    @Transactional
    public String issueAndSend(MembershipApplication application) {
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

        MembershipApplication current = requireApprovedApplicationForUpdate(application.getId());
        if (!java.util.Objects.equals(current.getCreatedUserId(), application.getCreatedUserId())) {
            throw new MembershipFirstAccessConflictException("Membership provisioning changed concurrently.");
        }
        pendingAuthentication.requirePending(current.getCreatedUserId());
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
        String link = linkFactory.create(rawToken);
        invitationDelivery.afterCommit(application.getId(), token.getTokenHash(), recipient.email(), recipient.name(), link);
        return link;
    }

    private MembershipApplication requireApprovedApplicationForUpdate(Long id) {
        MembershipApplication application = applicationRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new MembershipApplicationNotFoundException(id));
        if (application.getStatus() != MembershipApplicationStatus.APPROVED) {
            throw new MembershipFirstAccessConflictException("Membership application is not approved for activation.");
        }
        return application;
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
