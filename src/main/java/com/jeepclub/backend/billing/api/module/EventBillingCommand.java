package com.jeepclub.backend.billing.api.module;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Transactional financial operations; participation decisions belong to Publications. */
public interface EventBillingCommand {
    Long createOneTimeEventCharge(Long eventId, String name, String description, BigDecimal amount, boolean required);
    void ensureAssignment(Long eventId, Long definitionId);
    Long ensureEventMemberCharge(Long eventId, Long definitionId, Long userId, LocalDate dueDate, Long actorId);
    void cancelEventCycles(Long eventId, Long actorId);
}
