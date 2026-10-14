package com.jeepclub.backend.iam.authentication.infra.mail;

import com.jeepclub.backend.iam.authentication.core.application.service.activation.AccountActivationMailEvent;
import com.jeepclub.backend.iam.authentication.core.repository.AccountActivationInvitationRepository;
import com.jeepclub.backend.iam.identity.api.module.UserIdentityLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

/** Hold Identity while sending, so a queued event cannot send to an obsolete address. */
@Component
@RequiredArgsConstructor
public class AccountActivationCurrentDelivery {
    private final UserIdentityLock users;
    private final AccountActivationInvitationRepository invitations;
    private final AccountActivationMailSender sender;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean sendIfCurrent(AccountActivationMailEvent event) {
        var user = users.byId(event.userId()).orElse(null);
        var invitation = invitations.findById(event.userId()).orElse(null);
        if (user == null || !user.administrativelyActive() || !Objects.equals(user.email(), event.email())
                || invitation == null
                || invitation.deliveryStatus().equals("INVALIDATED") || invitation.usedAt() != null) return false;
        sender.send(user.email(), user.name(), event.rawToken());
        return true;
    }
}
