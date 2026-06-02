package com.jeepclub.backend.billing.infra.integration.event;

import com.jeepclub.backend.billing.core.port.BillingEventPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class UnavailableBillingEventAdapter implements BillingEventPort {

    @Override
    public boolean existsEventById(Long eventId) {
        Objects.requireNonNull(eventId, "eventId cannot be null");

        return false;
    }

    @Override
    public List<Long> findConfirmedParticipantUserIdsByEventId(Long eventId) {
        Objects.requireNonNull(eventId, "eventId cannot be null");

        return List.of();
    }
}