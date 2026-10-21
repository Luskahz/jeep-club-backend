package com.jeepclub.backend.memberships.infra.integration.identity;

import com.jeepclub.backend.iam.identity.api.module.UserAdministration;
import com.jeepclub.backend.iam.identity.api.module.UserIdentityLock;
import com.jeepclub.backend.iam.identity.api.module.exception.UserDeletionConflictException;
import com.jeepclub.backend.iam.identity.api.module.exception.UserNotFoundException;
import com.jeepclub.backend.memberships.core.application.exception.MembershipFirstAccessConflictException;
import com.jeepclub.backend.memberships.core.port.PendingMembershipIdentityReversal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class PendingMembershipIdentityReversalAdapter implements PendingMembershipIdentityReversal {
    private final UserIdentityLock identityLock;
    private final UserAdministration administration;
    @Override public void lock(Long userId) {
        if (identityLock.byId(userId).isEmpty()) throw new MembershipFirstAccessConflictException("Provisioned Identity is no longer available.");
    }
    @Override public void delete(Long userId, Long actorId, Instant now) {
        try { administration.delete(userId, actorId, now); }
        catch (UserDeletionConflictException | UserNotFoundException conflict) {
            throw new MembershipFirstAccessConflictException("Provisioned Identity cannot be safely reversed; retained relationships or completed access require review.");
        }
    }
}
