package com.jeepclub.backend.memberships.core.port;

import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import java.time.Instant;

/** Minimal independent evidence, without CPF, contact data, credentials or free text. */
public interface MembershipReviewHistory {
    void record(MembershipApplication before, String operation, String resultingStatus, Long originalUserId, Long actorId, Instant now);
}
