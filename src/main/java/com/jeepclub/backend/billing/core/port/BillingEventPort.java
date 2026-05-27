package com.jeepclub.backend.billing.core.port;

import java.util.List;

public interface BillingEventPort {

    boolean existsEventById(Long eventId);

    List<Long> findConfirmedParticipantUserIdsByEventId(Long eventId);
}