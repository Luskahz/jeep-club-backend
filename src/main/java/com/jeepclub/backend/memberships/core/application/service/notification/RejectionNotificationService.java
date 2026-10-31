package com.jeepclub.backend.memberships.core.application.service.notification;

import com.jeepclub.backend.memberships.core.application.exception.RejectionNotificationException;
import com.jeepclub.backend.memberships.core.port.MemberActivationMailSender;
import com.jeepclub.backend.memberships.core.port.RejectionNotificationLease;
import com.jeepclub.backend.memberships.core.repository.RejectionNotificationRepository;
import com.jeepclub.backend.memberships.core.repository.RejectionNotificationRepository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Clock;

import static com.jeepclub.backend.memberships.core.application.exception.RejectionNotificationException.Kind.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RejectionNotificationService {
    private final RejectionNotificationRepository notifications;
    private final MemberActivationMailSender mail;
    private final Clock clock;
    private final RejectionNotificationLease leases;

    public Status status(Long applicationId) { return notifications.status(applicationId); }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void process(Long notificationId) {
        notifications.claimAutomatic(notificationId, clock.instant()).ifPresent(this::send);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void poll() {
        for (Long id : notifications.due(clock.instant(), 50)) {
            try { process(id); }
            catch (RuntimeException failure) {
                log.warn("Membership rejection worker failed; notificationId={}; result=UNKNOWN", id);
            }
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Result resend(Long applicationId, Long actorId) {
        Result result = send(notifications.claimManual(applicationId, actorId, clock.instant()));
        if (result != Result.SMTP_ACCEPTED && result != Result.SIMULATED) throw new RejectionNotificationException(PROVIDER_FAILED);
        return result;
    }

    private Result send(Reservation reservation) {
        var application = reservation.application();
        Result result;
        try (var lease = leases.hold(reservation)) {
            mail.sendRejectionNotice(application.getEmail(), application.getName(), application.getRejectionReason());
            result = mail.isSimulated() ? Result.SIMULATED : Result.SMTP_ACCEPTED;
        } catch (RuntimeException failure) {
            // SMTP exceptions can retain personal data and entire messages.
            result = Result.SMTP_FAILED;
        }
        try { notifications.complete(reservation, result, clock.instant()); }
        catch (RuntimeException failure) {
            log.warn("Membership rejection result persistence failed; notificationId={}; result=UNKNOWN", reservation.notificationId());
            throw new RejectionNotificationException(RESULT_UNKNOWN);
        }
        if (result == Result.SMTP_FAILED) {
            log.warn("Membership rejection attempt failed; notificationId={}; result=SMTP_FAILED", reservation.notificationId());
        }
        return result;
    }
}
