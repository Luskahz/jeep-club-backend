package com.jeepclub.backend.memberships.infra.mail;

import com.jeepclub.backend.memberships.core.port.MemberActivationMailSender;
import com.jeepclub.backend.memberships.core.port.MembershipRejectionNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@RequiredArgsConstructor
@Slf4j
public class AfterCommitMembershipRejectionNotifier implements MembershipRejectionNotifier {
    private final ApplicationEventPublisher events;
    private final MemberActivationMailSender mail;
    // Never render this event's personal data via logging or tracing.
    private record Notice(String email, String name, String reason) {
        @Override public String toString() { return "MembershipRejectionNotice[REDACTED]"; }
    }
    @Override public void afterCommit(String email, String name, String reason) { events.publishEvent(new Notice(email, name, reason)); }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deliver(Notice notice) {
        try { mail.sendRejectionNotice(notice.email(), notice.name(), notice.reason()); }
        catch (RuntimeException failure) { log.warn("Membership rejection committed; notification delivery failed"); }
    }
}
