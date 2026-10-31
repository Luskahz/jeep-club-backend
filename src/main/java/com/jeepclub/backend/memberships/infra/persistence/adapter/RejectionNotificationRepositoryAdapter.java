package com.jeepclub.backend.memberships.infra.persistence.adapter;

import com.jeepclub.backend.memberships.core.application.exception.*;
import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import com.jeepclub.backend.memberships.core.repository.*;
import com.jeepclub.backend.memberships.infra.persistence.entity.*;
import com.jeepclub.backend.memberships.infra.persistence.jpa.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;

import static com.jeepclub.backend.memberships.core.application.exception.RejectionNotificationException.Kind.*;

@Repository
@RequiredArgsConstructor
public class RejectionNotificationRepositoryAdapter implements RejectionNotificationRepository {
    // Longer than the configured SMTP connection/read/write timeouts combined.
    private static final Duration LEASE = Duration.ofMinutes(5);
    private static final Duration RETRY = Duration.ofMinutes(2);
    private static final int MAX_AUTOMATIC = 3;
    private final RejectionNotificationJpaRepository notifications;
    private final RejectionNotificationAttemptJpaRepository attempts;
    private final MembershipApplicationRepository applications;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Status status(Long applicationId) {
        applications.findById(applicationId).orElseThrow(() -> new MembershipApplicationNotFoundException(applicationId));
        var row = notifications.findByApplicationId(applicationId).orElseThrow(() -> new RejectionNotificationException(NOT_FOUND));
        return new Status(row.getId(), row.getState(), row.getAutomaticAttempts(), row.getNextAttemptAt(),
                row.getSmtpAcceptedAt(), row.getLastResult(), attempts.findByNotificationIdOrderById(row.getId()).stream()
                .map(a -> new Attempt(a.getId(), a.getOrigin(), a.getActorId(), a.getAutomaticNumber(),
                        a.getResult(), a.getStartedAt(), a.getFinishedAt())).toList());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Long enqueue(MembershipApplication application, Instant now) {
        requireRejected(application);
        return create(application, now, true).getId();
    }

    private RejectionNotificationEntity create(MembershipApplication application, Instant now, boolean automatic) {
        var row = new RejectionNotificationEntity();
        row.setApplicationId(application.getId());
        row.setDecisionVersion(application.getVersion());
        row.setCreatedAt(now); row.setUpdatedAt(now);
        if (automatic) row.setNextAttemptAt(now);
        else row.setState("FAILED"); // Explicit manual recovery of a historical rejection: no backfill cycle.
        return notifications.saveAndFlush(row);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<Long> due(Instant now, int limit) { return notifications.due(now, PageRequest.of(0, limit)); }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Reservation> claimAutomatic(Long id, Instant now) {
        var row = notifications.lock(id).orElse(null);
        if (row == null) return Optional.empty();
        recover(row, now);
        if (!row.getState().equals("PENDING") || row.getNextAttemptAt() == null
                || row.getNextAttemptAt().isAfter(now) || row.getAutomaticAttempts() >= MAX_AUTOMATIC) return Optional.empty();
        var application = applications.findById(row.getApplicationId()).orElse(null);
        if (!eligible(application, row)) {
            row.setState("FAILED"); row.setNextAttemptAt(null); row.setLastResult("INELIGIBLE"); row.setUpdatedAt(now);
            return Optional.empty();
        }
        return Optional.of(reserve(row, application, Origin.AUTOMATIC, null, now));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Reservation claimManual(Long applicationId, Long actorId, Instant now) {
        Objects.requireNonNull(actorId, "Administrative actor is required");
        // Serializes creation for explicitly requested historical recovery as well.
        var application = applications.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new MembershipApplicationNotFoundException(applicationId));
        requireRejected(application);
        if (application.getEmail() == null || application.getEmail().isBlank()) throw new RejectionNotificationException(NO_RECIPIENT);
        var row = notifications.lockByApplicationId(applicationId).orElseGet(() -> create(application, now, false));
        recover(row, now);
        if (row.getState().equals("PROCESSING")) throw new RejectionNotificationException(PROCESSING);
        if (!eligible(application, row)) throw new RejectionNotificationException(NOT_REJECTED);
        return reserve(row, application, Origin.MANUAL, actorId, now);
    }

    private Reservation reserve(RejectionNotificationEntity row, MembershipApplication application, Origin origin, Long actor, Instant now) {
        var attempt = new RejectionNotificationAttemptEntity();
        attempt.setNotificationId(row.getId()); attempt.setOrigin(origin.name()); attempt.setActorId(actor);
        if (origin == Origin.AUTOMATIC) {
            row.setAutomaticAttempts(row.getAutomaticAttempts() + 1);
            attempt.setAutomaticNumber(row.getAutomaticAttempts());
        }
        attempt.setResult("PROCESSING"); attempt.setStartedAt(now);
        attempts.saveAndFlush(attempt);
        row.setState("PROCESSING"); row.setLeaseUntil(now.plus(LEASE)); row.setCurrentAttemptId(attempt.getId());
        row.setLastOrigin(origin.name()); row.setLastResult("PROCESSING"); row.setLastAttemptAt(now); row.setUpdatedAt(now);
        return new Reservation(row.getId(), attempt.getId(), application);
    }

    private void recover(RejectionNotificationEntity row, Instant now) {
        if (!row.getState().equals("PROCESSING") || row.getLeaseUntil().isAfter(now)) return;
        var attempt = attempts.findById(row.getCurrentAttemptId()).orElseThrow();
        attempt.setResult("UNKNOWN"); attempt.setFinishedAt(now);
        settle(row, Result.UNKNOWN, now);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(Reservation reservation, Result result, Instant now) {
        var row = notifications.lock(reservation.notificationId()).orElseThrow();
        if (!row.getState().equals("PROCESSING") || !Objects.equals(row.getCurrentAttemptId(), reservation.attemptId())) {
            throw new RejectionNotificationException(RESULT_UNKNOWN); // A stale lease cannot overwrite a newer outcome.
        }
        var attempt = attempts.findById(reservation.attemptId()).orElseThrow();
        attempt.setResult(result.name()); attempt.setFinishedAt(now);
        settle(row, result, now);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean renew(Reservation reservation, Instant now) {
        var row = notifications.lock(reservation.notificationId()).orElseThrow();
        if (!row.getState().equals("PROCESSING") || !Objects.equals(row.getCurrentAttemptId(), reservation.attemptId())) return false;
        row.setLeaseUntil(now.plus(LEASE)); row.setUpdatedAt(now);
        return true;
    }

    private void settle(RejectionNotificationEntity row, Result result, Instant now) {
        row.setLastResult(result.name()); row.setUpdatedAt(now); row.setLeaseUntil(null); row.setCurrentAttemptId(null);
        if (result == Result.SMTP_ACCEPTED || result == Result.SIMULATED) {
            if (result == Result.SMTP_ACCEPTED) row.setSmtpAcceptedAt(now);
            row.setState("SENT"); row.setNextAttemptAt(null);
        } else if (row.getSmtpAcceptedAt() != null) {
            row.setState("SENT"); row.setNextAttemptAt(null); // A failed manual repeat cannot undo prior acceptance.
        } else if (row.getAutomaticAttempts() < MAX_AUTOMATIC && row.getNextAttemptAt() != null) {
            row.setState("PENDING");
            if (row.getLastOrigin().equals("AUTOMATIC")) row.setNextAttemptAt(now.plus(RETRY));
        } else { row.setState("FAILED"); row.setNextAttemptAt(null); }
    }

    private boolean eligible(MembershipApplication application, RejectionNotificationEntity row) {
        return application != null && application.getStatus() == MembershipApplicationStatus.REJECTED
                && Objects.equals(application.getVersion(), row.getDecisionVersion())
                && application.getEmail() != null && !application.getEmail().isBlank();
    }
    private void requireRejected(MembershipApplication application) {
        if (application.getStatus() != MembershipApplicationStatus.REJECTED) throw new RejectionNotificationException(NOT_REJECTED);
    }
}
