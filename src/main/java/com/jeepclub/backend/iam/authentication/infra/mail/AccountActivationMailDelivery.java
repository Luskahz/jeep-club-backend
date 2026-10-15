package com.jeepclub.backend.iam.authentication.infra.mail;

import com.jeepclub.backend.iam.authentication.core.application.service.activation.AccountActivationMailEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AccountActivationMailDelivery {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountActivationMailDelivery.class);
    private final AccountActivationCurrentDelivery delivery;
    private final AccountActivationDeliveryStatusWriter statusWriter;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deliver(AccountActivationMailEvent event) {
        String status;
        try {
            if (!delivery.sendIfCurrent(event)) return;
            status = "SENT";
        } catch (RuntimeException ignored) {
            status = "DELIVERY_FAILED";
        }
        try {
            statusWriter.update(event.userId(), event.tokenHash(), status);
        } catch (RuntimeException ignored) {
            // A committed account remains recoverable by an administrator issuing a new invitation.
            LOGGER.warn("Account activation delivery status could not be persisted");
        }
    }
}
