package com.jeepclub.backend.memberships.infra.mail;

import com.jeepclub.backend.memberships.core.application.service.notification.RejectionNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
@ConditionalOnProperty(name = "memberships.notifications.worker.enabled", havingValue = "true", matchIfMissing = true)
public class RejectionNotificationWorker {
    private final RejectionNotificationService service;
    private final ThreadPoolTaskExecutor executor;
    private final AtomicBoolean polling = new AtomicBoolean();

    public RejectionNotificationWorker(RejectionNotificationService service) {
        this.service = service;
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("membership-rejection-");
        executor.initialize();
    }

    @TransactionalEventListener
    public void committed(DurableMembershipRejectionNotifier.Queued queued) {
        try {
            executor.execute(() -> {
                try { service.process(queued.notificationId()); }
                catch (RuntimeException failure) { log.warn("Membership rejection processing deferred; result=UNKNOWN"); }
            });
        } catch (RuntimeException unavailable) {
            log.warn("Membership rejection dispatch deferred to durable polling");
        }
    }

    @Scheduled(fixedDelayString = "${memberships.notifications.poll-delay-ms:1000}")
    public void poll() {
        if (!polling.compareAndSet(false, true)) return;
        try {
            executor.execute(() -> {
                try { service.poll(); }
                catch (RuntimeException failure) { log.warn("Membership rejection polling failed; result=UNKNOWN"); }
                finally { polling.set(false); }
            });
        } catch (RuntimeException unavailable) {
            polling.set(false);
            log.warn("Membership rejection polling deferred");
        }
    }

    @PreDestroy public void close() { executor.shutdown(); }
}
