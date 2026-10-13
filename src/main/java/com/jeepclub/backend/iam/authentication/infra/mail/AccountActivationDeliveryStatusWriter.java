package com.jeepclub.backend.iam.authentication.infra.mail;

import com.jeepclub.backend.iam.authentication.infra.persistence.jpa.AccountActivationInvitationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AccountActivationDeliveryStatusWriter {
    private final AccountActivationInvitationJpaRepository invitations;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void update(Long userId, String tokenHash, String status) {
        // The token hash predicate is checked by the database while updating.
        // Delivery state does not advance the invitation's optimistic version:
        // a concurrent token rotation must not fail because SMTP reported a result.
        invitations.updateDeliveryStatusIfCurrent(userId, tokenHash, status);
    }
}
