package com.jeepclub.backend.memberships.core.port;

import com.jeepclub.backend.memberships.core.repository.RejectionNotificationRepository.Reservation;

/** Renews ownership while an external delivery is running. */
public interface RejectionNotificationLease {
    interface Handle extends AutoCloseable { @Override void close(); }
    Handle hold(Reservation reservation);
}
