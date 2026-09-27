package com.jeepclub.backend.publications.core.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record EventChargeRule(Long eventId, Long chargeDefinitionId, boolean requiredForParticipation,
                              Instant participationCutoff, LocalDate financialDueDate) {
    public EventChargeRule {
        Publication.positive(eventId, "eventId");
        Publication.positive(chargeDefinitionId, "chargeDefinitionId");
        Objects.requireNonNull(participationCutoff, "participationCutoff");
    }
}
