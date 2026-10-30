package com.jeepclub.backend.memberships;

import com.jeepclub.backend.memberships.core.application.exception.RejectionNotificationException;
import com.jeepclub.backend.memberships.core.application.service.membershipapplication.AdminMembershipApplicationService;
import com.jeepclub.backend.memberships.core.application.service.notification.RejectionNotificationService;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import com.jeepclub.backend.memberships.core.port.MemberActivationMailSender;
import com.jeepclub.backend.memberships.core.repository.*;
import com.jeepclub.backend.memberships.core.repository.RejectionNotificationRepository.*;
import com.jeepclub.backend.memberships.infra.persistence.adapter.RejectionNotificationRepositoryAdapter;
import com.jeepclub.backend.memberships.infra.persistence.jpa.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(RejectionNotificationIntegrationTest.TimeConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RejectionNotificationIntegrationTest {
    static final Instant NOW = Instant.parse("2026-10-30T12:00:00Z");
    static final AtomicInteger IDS = new AtomicInteger(100000000);
    @TestConfiguration static class TimeConfiguration {
        @Bean @Primary MutableClock notificationClock() { return new MutableClock(); }
    }
    static class MutableClock extends Clock {
        final AtomicReference<Instant> now = new AtomicReference<>(NOW);
        void advance(long seconds) { now.updateAndGet(i -> i.plusSeconds(seconds)); }
        public Instant instant() { return now.get(); }
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
    }
    @Autowired MutableClock clock;
    @Autowired com.jeepclub.backend.memberships.core.port.RejectionNotificationLease leases;
    @Autowired MembershipApplicationRepository applications;
    @Autowired AdminMembershipApplicationService admin;
    @Autowired RejectionNotificationService service;
    @MockitoSpyBean RejectionNotificationRepositoryAdapter queue;
    @Autowired RejectionNotificationJpaRepository rows;
    @Autowired RejectionNotificationAttemptJpaRepository attempts;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean MemberActivationMailSender mail;

    @BeforeEach void time() { clock.now.set(NOW); }
    Long pending() {
        return new TransactionTemplate(transactions).execute(tx -> applications.saveAndFlush(MembershipApplication.create(
                "Synthetic candidate", "71" + IDS.incrementAndGet(), "synthetic@example.com", "11999999999", null, clock.instant())).getId());
    }
    Long reject(String reason) { Long id = pending(); admin.reject(id, 7L, reason); return id; }
    Long notification(Long application) { return queue.status(application).notificationId(); }
    void failSmtp() { doThrow(new org.springframework.mail.MailSendException("private recipient and reason"))
            .when(mail).sendRejectionNotice(any(), any(), nullable(String.class)); }

    @Test void rejectionAndOutboxRollbackTogetherWithNoSmtp() {
        Long id = pending(); long count = rows.count(); long history = attempts.count();
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            admin.rejectAndBlock(id, 7L, "Synthetic reason");
            assertThat(rows.findByApplicationId(id)).isPresent();
            verifyNoInteractions(mail); tx.setRollbackOnly();
        });
        assertThat(rows.count()).isEqualTo(count); assertThat(attempts.count()).isEqualTo(history);
        assertThat(applications.findById(id).orElseThrow().getStatus().name()).isEqualTo("PENDING");
        verifyNoInteractions(mail);
    }

    @Test void threeTotalAutomaticAttemptsAtTwoMinuteIntervalsAndManualDoesNotRestartCycle() {
        Long app = reject("  persisted reason  "); Long id = notification(app); failSmtp();
        service.process(id);
        var first = queue.status(app);
        assertThat(first.state()).isEqualTo("PENDING"); assertThat(first.automaticAttempts()).isEqualTo(1);
        assertThat(first.nextAttemptAt()).isEqualTo(NOW.plusSeconds(120));
        clock.advance(119); service.process(id); assertThat(queue.status(app).automaticAttempts()).isEqualTo(1);
        clock.advance(1); service.process(id);
        assertThat(queue.status(app).nextAttemptAt()).isEqualTo(NOW.plusSeconds(240));
        clock.advance(120); service.process(id); service.process(id);
        assertThat(queue.status(app).state()).isEqualTo("FAILED");
        assertThat(queue.status(app).automaticAttempts()).isEqualTo(3);
        assertThat(queue.status(app).attempts()).hasSize(3).allSatisfy(a -> assertThat(a.result()).isEqualTo("SMTP_FAILED"));
        assertThatThrownBy(() -> service.resend(app, 91L)).isInstanceOf(RejectionNotificationException.class);
        assertThat(queue.status(app).automaticAttempts()).isEqualTo(3);
        reset(mail); service.resend(app, 92L);
        assertThat(queue.status(app).state()).isEqualTo("SENT");
        assertThat(queue.status(app).attempts()).hasSize(5).last().satisfies(a -> {
            assertThat(a.origin()).isEqualTo("MANUAL"); assertThat(a.actorId()).isEqualTo(92L);
        });
        assertThat(applications.findById(app).orElseThrow().getRejectionReason()).isEqualTo("persisted reason");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @org.junit.jupiter.params.provider.ValueSource(strings = {"   ", "  Original persisted reason  "})
    void providerAcceptanceStopsAllAutomaticRetriesAndUsesTheOriginalDecision(String reason) {
        Long app = reject(reason); Long id = notification(app);
        doAnswer(call -> {
            // Claim has already committed: another claimant immediately sees PROCESSING, without waiting for SMTP.
            assertThat(queue.claimAutomatic(id, clock.instant())).isEmpty();
            assertThat(queue.status(app).state()).isEqualTo("PROCESSING");
            return null;
        }).when(mail).sendRejectionNotice(any(), any(), nullable(String.class));
        service.process(id); clock.advance(600); service.process(id);
        assertThat(queue.status(app).state()).isEqualTo("SENT"); assertThat(queue.status(app).attempts()).hasSize(1);
        verify(mail).sendRejectionNotice("synthetic@example.com", "Synthetic candidate", reason == null || reason.isBlank() ? null : reason.strip());
    }

    @Test void manualAttemptIsImmediateAndFailurePreservesTheOriginalAutomaticSchedule() {
        Long app = reject(null); Long id = notification(app); failSmtp(); service.process(id);
        clock.advance(30);
        assertThatThrownBy(() -> service.resend(app, 88L)).isInstanceOf(RejectionNotificationException.class);
        assertThat(queue.status(app).nextAttemptAt()).isEqualTo(NOW.plusSeconds(120));
        assertThat(queue.status(app).automaticAttempts()).isEqualTo(1);
        reset(mail); service.resend(app, 88L); clock.advance(200); service.process(id);
        assertThat(queue.status(app).state()).isEqualTo("SENT"); assertThat(queue.status(app).attempts()).hasSize(3);
        verify(mail, times(1)).sendRejectionNotice(any(), any(), nullable(String.class));
    }

    @Test void freshDispatcherRecoversPersistentPendingWorkWithoutInMemoryEvents() {
        Long app = reject("Original"); Long id = notification(app); failSmtp(); service.process(id);
        reset(mail); clock.advance(120);
        new RejectionNotificationService(queue, mail, clock, leases).process(id);
        assertThat(queue.status(app).state()).isEqualTo("SENT"); assertThat(queue.status(app).automaticAttempts()).isEqualTo(2);
    }

    @Test void expiredLeaseRecordsAmbiguityAndCountsAbandonedAttemptsTowardTheOriginalLimit() {
        Long app = reject(null); Long id = notification(app);
        var old = queue.claimAutomatic(id, clock.instant()).orElseThrow();
        assertThat(queue.claimAutomatic(id, clock.instant())).isEmpty();
        clock.advance(300); service.process(id);
        assertThat(queue.status(app).lastResult()).isEqualTo("UNKNOWN");
        assertThat(queue.status(app).attempts().get(0).result()).isEqualTo("UNKNOWN");
        clock.advance(120); var second = queue.claimAutomatic(id, clock.instant()).orElseThrow();
        assertThatThrownBy(() -> queue.complete(old, Result.SMTP_ACCEPTED, clock.instant())).isInstanceOf(RejectionNotificationException.class);
        clock.advance(300); service.process(id); clock.advance(120);
        queue.claimAutomatic(id, clock.instant()).orElseThrow(); clock.advance(300); service.process(id);
        assertThat(queue.status(app).automaticAttempts()).isEqualTo(3); assertThat(queue.status(app).state()).isEqualTo("FAILED");
        verifyNoInteractions(mail);
    }

    @Test void acceptedSmtpAndFailedResultWriteRemainsRecoverableAndExplicitlyUnknown() {
        Long app = reject(null); Long id = notification(app);
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("synthetic result failure"))
                .when(queue).complete(any(), eq(Result.SMTP_ACCEPTED), any());
        try { assertThatThrownBy(() -> service.resend(app, 8L)).isInstanceOf(RejectionNotificationException.class)
                    .satisfies(f -> assertThat(((RejectionNotificationException) f).getKind()).isEqualTo(RejectionNotificationException.Kind.RESULT_UNKNOWN)); }
        finally { reset(queue); }
        assertThat(queue.status(app).state()).isEqualTo("PROCESSING");
        clock.advance(300); service.process(id);
        assertThat(queue.status(app).attempts().get(0).result()).isEqualTo("UNKNOWN");
        clock.advance(120); service.process(id); assertThat(queue.status(app).state()).isEqualTo("SENT");
    }

    @Test void twoWorkersAndAManualRequestCannotClaimTheSameLiveAttempt() throws Exception {
        Long app = reject("Original"); Long id = notification(app);
        var sending = new CountDownLatch(1); var release = new CountDownLatch(1);
        doAnswer(call -> { sending.countDown(); assertThat(release.await(15, TimeUnit.SECONDS)).isTrue(); return null; })
                .when(mail).sendRejectionNotice(any(), any(), any());
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> new RejectionNotificationService(queue, mail, clock, leases).process(id));
            assertThat(sending.await(15, TimeUnit.SECONDS)).isTrue();
            pool.submit(() -> new RejectionNotificationService(queue, mail, clock, leases).process(id)).get(10, TimeUnit.SECONDS);
            assertThatThrownBy(() -> service.resend(app, 9L)).isInstanceOf(RejectionNotificationException.class)
                    .satisfies(f -> assertThat(((RejectionNotificationException)f).getKind()).isEqualTo(RejectionNotificationException.Kind.PROCESSING));
            release.countDown(); first.get(15, TimeUnit.SECONDS);
        } finally { release.countDown(); pool.shutdownNow(); }
        verify(mail, times(1)).sendRejectionNotice(any(), any(), any());
        assertThat(queue.status(app).attempts()).hasSize(1);
    }

    @Test void simulatedProfileCannotBeReportedAsSmtpAcceptance() {
        Long app = reject(null); when(mail.isSimulated()).thenReturn(true);
        assertThat(service.resend(app, 8L)).isEqualTo(Result.SIMULATED);
        assertThat(queue.status(app).state()).isEqualTo("SENT");
        assertThat(queue.status(app).smtpAcceptedAt()).isNull();
        assertThat(queue.status(app).lastResult()).isEqualTo("SIMULATED");
    }

    @Test void aFreshWorkerPollsDurablePendingWorkWithoutTheOriginalEvent() throws Exception {
        Long app = reject(null); Long id = notification(app); failSmtp(); service.process(id);
        reset(mail); clock.advance(120);
        var worker = new com.jeepclub.backend.memberships.infra.mail.RejectionNotificationWorker(service);
        try {
            worker.poll();
            verify(mail, timeout(5000)).sendRejectionNotice(any(), any(), nullable(String.class));
        } finally { worker.close(); }
    }

    @Test void leaseRenewalPreventsReclaimDuringALongRunningLiveAttempt() {
        Long app = reject(null); Long id = notification(app);
        var active = queue.claimAutomatic(id, clock.instant()).orElseThrow();
        clock.advance(290); assertThat(queue.renew(active, clock.instant())).isTrue();
        clock.advance(20); assertThat(queue.claimAutomatic(id, clock.instant())).isEmpty();
        assertThatThrownBy(() -> queue.claimManual(app, 9L, clock.instant())).isInstanceOf(RejectionNotificationException.class);
        queue.complete(active, Result.SMTP_ACCEPTED, clock.instant());
        assertThat(queue.renew(active, clock.instant())).isFalse();
    }

    @Test void historicalRejectionIsOnlyQueuedWhenManuallyRequestedWithoutStartingAnAutomaticCycle() {
        Long app = pending();
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            var row = applications.findByIdForUpdate(app).orElseThrow(); row.reject(7L, null, clock.instant()); applications.saveAndFlush(row);
        });
        assertThat(rows.findByApplicationId(app)).isEmpty(); failSmtp();
        assertThatThrownBy(() -> service.resend(app, 8L)).isInstanceOf(RejectionNotificationException.class);
        assertThat(queue.status(app).state()).isEqualTo("FAILED"); assertThat(queue.status(app).automaticAttempts()).isZero();
        assertThat(queue.status(app).nextAttemptAt()).isNull();
    }
}
