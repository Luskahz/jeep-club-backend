package com.jeepclub.backend.iam.authentication.core.application.service.activation;

import com.jeepclub.backend.iam.identity.api.module.UserIdentityLock;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.account.ActivationInvitationException;
import com.jeepclub.backend.iam.authentication.core.application.service.internal.CredentialRevocationService;
import com.jeepclub.backend.iam.authentication.core.domain.model.AuthenticationAccount;
import com.jeepclub.backend.iam.authentication.core.domain.model.AccountActivationInvitation;
import com.jeepclub.backend.iam.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.iam.authentication.core.port.RefreshTokenGenerator;
import com.jeepclub.backend.iam.authentication.core.port.RefreshTokenHashService;
import com.jeepclub.backend.iam.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.iam.authentication.core.repository.AccountActivationInvitationRepository;
import com.jeepclub.backend.iam.identity.api.module.UserDetails;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.memberships.api.module.MembershipIdentityOwnership;
import com.jeepclub.backend.platform.logging.SystemLogEvent;
import com.jeepclub.backend.platform.logging.SystemLogOutcome;
import com.jeepclub.backend.platform.logging.SystemLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AccountActivationInvitationService {
    private final AccountActivationInvitationRepository invitations;
    private final AuthenticationAccountRepository accounts;
    private final UserQuery users;
    private final UserIdentityLock identityLock;
    private final MembershipIdentityOwnership membershipIdentityOwnership;
    private final RefreshTokenGenerator tokenGenerator;
    private final RefreshTokenHashService tokenHashService;
    private final PasswordHasher passwordHasher;
    private final CredentialRevocationService revocations;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final SystemLogService audit;

    @Value("${app.authentication.account-activation-ttl:PT24H}")
    private Duration ttl;

    @Transactional
    public void issueForNewUser(Long userId) {
        issue(userId);
    }

    @Transactional
    public void issueForExistingUser(Long userId, Long actorId) {
        java.util.Objects.requireNonNull(actorId, "actorId is required for administrative audit");
        issue(userId);
        audit.recordInTransaction(new SystemLogEvent(actorId, "ACCOUNT_ACTIVATION_EMAIL_INVITATION_REISSUE",
                "POST", "/authentication/admin/users/" + userId + "/activation-invitations",
                202, SystemLogOutcome.SUCCESS, 0, null, Instant.now(clock)));
    }

    @Transactional(readOnly = true)
    public String deliveryStatus(Long userId) {
        // Scalar projection reads the committed status rather than a request-scoped
        // EntityManager's pre-AFTER_COMMIT snapshot of the invitation entity.
        return invitations.findDeliveryStatusById(userId)
                .orElseThrow(() -> new ActivationInvitationException(true));
    }

    private void issue(Long userId) {
        UserDetails user = identityLock.byId(userId)
                .orElseThrow(() -> new ActivationInvitationException(true));
        AuthenticationAccount account = accounts.findByIdentityIdForUpdate(userId)
                .orElseThrow(() -> new ActivationInvitationException(true));
        if (!account.isPendingFirstAccess() || account.isBlockedForLogin() || !user.administrativelyActive()
                || user.email() == null || user.email().isBlank()
                || membershipIdentityOwnership.ownsIdentity(userId)) {
            throw new ActivationInvitationException(true);
        }
        String rawToken = tokenGenerator.generate();
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalStateException("Account activation TTL must be positive.");
        }
        String hash = tokenHashService.hash(rawToken);
        Instant expiresAt = Instant.now(clock).plus(ttl);
        AccountActivationInvitation invitation = invitations.findById(userId)
                .orElseGet(() -> AccountActivationInvitation.create(userId, hash, expiresAt));
        invitation.rotate(hash, expiresAt);
        invitations.save(invitation);
        events.publishEvent(new AccountActivationMailEvent(userId, user.email(), user.name(), rawToken, hash));
    }

    @Transactional(readOnly = true)
    public void validate(String rawToken) {
        AccountActivationInvitation invitation = invitations.findByTokenHash(tokenHashService.hash(rawToken))
                .orElseThrow(() -> new ActivationInvitationException(false));
        requireValid(invitation);
    }

    @Transactional
    public void complete(String rawToken, String newPassword) {
        String hash = tokenHashService.hash(rawToken);
        // Account and invitation mutations share a fixed lock order with issue/reissue.
        Long identityId = invitations.findIdentityIdByTokenHash(hash)
                .orElseThrow(() -> new ActivationInvitationException(false));
        identityLock.byId(identityId).orElseThrow(() -> new ActivationInvitationException(false));
        AuthenticationAccount account = accounts.findByIdentityIdForUpdate(identityId)
                .orElseThrow(() -> new ActivationInvitationException(false));
        AccountActivationInvitation invitation = invitations.findByTokenHashForUpdate(hash)
                .orElseThrow(() -> new ActivationInvitationException(false));
        requireValid(invitation);
        Instant now = Instant.now(clock);
        account.changePassword(passwordHasher.hash(newPassword), now);
        revocations.revokeAllForUser(account.getIdentityId(), now);
        accounts.save(account);
        invitation.markUsed(now);
        invitations.save(invitation);
    }

    private void requireValid(AccountActivationInvitation invitation) {
        AuthenticationAccount account = accounts.findByIdentityId(invitation.identityId())
                .orElseThrow(() -> new ActivationInvitationException(false));
        if (invitation.usedAt() != null || !Instant.now(clock).isBefore(invitation.expiresAt())
                || !account.isPendingFirstAccess() || account.isBlockedForLogin()
                || !users.isAdministrativelyActive(invitation.identityId())
                || membershipIdentityOwnership.ownsIdentity(invitation.identityId())) {
            throw new ActivationInvitationException(false);
        }
    }
}
