package com.jeepclub.backend.memberships.core.repository;

import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RejectionNotificationRepository {
    enum Origin { AUTOMATIC, MANUAL }
    enum Result { SMTP_ACCEPTED, SIMULATED, SMTP_FAILED, UNKNOWN, INELIGIBLE }
    record Reservation(Long notificationId, Long attemptId, MembershipApplication application) {
        @Override public String toString() { return "RejectionReservation[notificationId=" + notificationId + "]"; }
    }
    record Attempt(Long id, String origin, Long actorId, Integer automaticNumber, String result,
                   Instant startedAt, Instant finishedAt) { }
    record Status(Long notificationId, String state, int automaticAttempts, Instant nextAttemptAt,
                  Instant smtpAcceptedAt, String lastResult, List<Attempt> attempts) { }
    Long enqueue(MembershipApplication application, Instant now);
    List<Long> due(Instant now, int limit);
    Optional<Reservation> claimAutomatic(Long notificationId, Instant now);
    Reservation claimManual(Long applicationId, Long actorId, Instant now);
    void complete(Reservation reservation, Result result, Instant now);
    boolean renew(Reservation reservation, Instant now);
    Status status(Long applicationId);
}
