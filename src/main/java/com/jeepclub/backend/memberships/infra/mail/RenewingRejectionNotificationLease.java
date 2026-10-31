package com.jeepclub.backend.memberships.infra.mail;

import com.jeepclub.backend.memberships.core.port.RejectionNotificationLease;
import com.jeepclub.backend.memberships.core.repository.RejectionNotificationRepository;
import com.jeepclub.backend.memberships.core.repository.RejectionNotificationRepository.Reservation;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.util.concurrent.*;

@Component
@Slf4j
public class RenewingRejectionNotificationLease implements RejectionNotificationLease {
    private final RejectionNotificationRepository notifications;
    private final Clock clock;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "membership-rejection-lease"); thread.setDaemon(true); return thread;
    });
    public RenewingRejectionNotificationLease(RejectionNotificationRepository notifications, Clock clock) {
        this.notifications = notifications; this.clock = clock;
    }
    @Override public Handle hold(Reservation reservation) {
        var renewal = executor.scheduleAtFixedRate(() -> {
            try {
                if (!notifications.renew(reservation, clock.instant())) {
                    log.warn("Membership rejection lease lost; notificationId={}", reservation.notificationId());
                }
            } catch (RuntimeException failure) {
                log.warn("Membership rejection lease renewal failed; notificationId={}", reservation.notificationId());
            }
        }, 30, 30, TimeUnit.SECONDS);
        return () -> renewal.cancel(false);
    }
    @PreDestroy public void close() { executor.shutdownNow(); }
}
