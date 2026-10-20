package com.jeepclub.backend.memberships.infra.persistence.adapter;

import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import com.jeepclub.backend.memberships.core.port.MembershipReviewHistory;
import com.jeepclub.backend.memberships.infra.persistence.entity.MembershipApplicationReviewEntity;
import com.jeepclub.backend.memberships.infra.persistence.jpa.MembershipApplicationReviewJpaRepository;
import com.jeepclub.backend.platform.logging.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class MembershipReviewHistoryAdapter implements MembershipReviewHistory {
    private final MembershipApplicationReviewJpaRepository repository;
    private final SystemLogService audit;
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(MembershipApplication before, String operation, String resultingStatus, Long originalUserId, Long actorId, Instant now) {
        var row = new MembershipApplicationReviewEntity();
        row.setApplicationId(before.getId());row.setOperation(operation);
        row.setPreviousStatus(before.getStatus().name());row.setResultingStatus(resultingStatus);
        row.setPreviousReviewerId(before.getReviewedByUserId());row.setPreviousReviewedAt(before.getReviewedAt());
        row.setPreviousFinishedAt(before.getFinishedAt());row.setOriginalUserId(originalUserId);
        row.setActorId(actorId);row.setOccurredAt(now);
        repository.saveAndFlush(row);
        audit.recordInTransaction(new SystemLogEvent(actorId, "MEMBERSHIP_" + operation, "POST",
                "/admin/membership-applications/" + before.getId(), 200, SystemLogOutcome.SUCCESS, 0, null, now));
    }
}
