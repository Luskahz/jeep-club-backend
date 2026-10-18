package com.jeepclub.backend.memberships.core.domain.model;

import java.time.Instant;

public record MemberRegistryAudit(Long id, Long memberId, String previousNumber, String newNumber,
        Long actorUserId, Instant occurredAt, String reason, String operation, String outcome) { }
