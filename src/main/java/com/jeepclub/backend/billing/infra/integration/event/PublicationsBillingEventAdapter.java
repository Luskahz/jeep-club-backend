package com.jeepclub.backend.billing.infra.integration.event;

import com.jeepclub.backend.billing.core.port.BillingEventPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@lombok.RequiredArgsConstructor
public class PublicationsBillingEventAdapter implements BillingEventPort {
    private final com.jeepclub.backend.publications.api.module.EventQuery events;

    @Override
    public boolean existsEventById(Long eventId) {
        Objects.requireNonNull(eventId, "eventId cannot be null");

        return events.exists(eventId);
    }

    @Override
    public List<Long> findConfirmedParticipantUserIdsByEventId(Long eventId) {
        Objects.requireNonNull(eventId, "eventId cannot be null");

        return events.confirmedParticipants(eventId);
    }
}
