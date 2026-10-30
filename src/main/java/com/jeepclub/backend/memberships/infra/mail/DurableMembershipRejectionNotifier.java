package com.jeepclub.backend.memberships.infra.mail;

import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import com.jeepclub.backend.memberships.core.port.MembershipRejectionNotifier;
import com.jeepclub.backend.memberships.core.repository.RejectionNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import java.time.Clock;

@Component
@RequiredArgsConstructor
public class DurableMembershipRejectionNotifier implements MembershipRejectionNotifier {
    public record Queued(Long notificationId) { }
    private final RejectionNotificationRepository notifications;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(MembershipApplication application) {
        events.publishEvent(new Queued(notifications.enqueue(application, clock.instant())));
    }
}
