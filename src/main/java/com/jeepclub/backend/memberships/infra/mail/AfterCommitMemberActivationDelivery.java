package com.jeepclub.backend.memberships.infra.mail;

import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.port.*;
import com.jeepclub.backend.memberships.core.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;

import java.time.Clock;
import java.time.Instant;

@Component
@Slf4j
public class AfterCommitMemberActivationDelivery implements MemberActivationInvitationDelivery {
    private final MemberActivationMailSender mail;
    private final MembershipApplicationRepository applications;
    private final MemberActivationTokenRepository tokens;
    private final Clock clock;
    private final TransactionTemplate read;

    public AfterCommitMemberActivationDelivery(MemberActivationMailSender mail,
            MembershipApplicationRepository applications, MemberActivationTokenRepository tokens,
            Clock clock, PlatformTransactionManager transactions) {
        this.mail = mail;
        this.applications = applications;
        this.tokens = tokens;
        this.clock = clock;
        read = new TransactionTemplate(transactions);
        read.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        read.setReadOnly(true);
    }

    @Override
    public void afterCommit(Long applicationId, String tokenHash, String email, String name, String link) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Activation delivery requires a transaction");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try {
                    // Use a fresh transaction: the original persistence context has already committed.
                    boolean eligible = Boolean.TRUE.equals(read.execute(status -> {
                        var app = applications.findById(applicationId).orElse(null);
                        var token = tokens.findByTokenHash(tokenHash).orElse(null);
                        return app != null && app.getStatus() == MembershipApplicationStatus.APPROVED
                                && token != null && token.getUsedAt() == null
                                && token.getExpiresAt().isAfter(Instant.now(clock));
                    }));
                    if (eligible) mail.sendActivationLink(email, name, link);
                } catch (RuntimeException failure) {
                    // Provider exceptions may retain the complete message and bearer. Never log them.
                    log.warn("Membership activation committed; invitation attempt failed; applicationId={}", applicationId);
                }
            }
            @Override public String toString() { return "MembershipActivationDelivery[REDACTED]"; }
        });
    }
}
