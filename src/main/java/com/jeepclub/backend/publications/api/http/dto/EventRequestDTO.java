package com.jeepclub.backend.publications.api.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.math.BigDecimal;

@Schema(description="Event editorial fields and agenda. PATCH preserves omitted fields; explicit null is accepted only for endsAt. Charges lock after the first registration.")
public record EventRequestDTO(String title, String content, List<PublicationImageRequestDTO> images,
        Instant startsAt, Instant endsAt, List<Charge> charges) {
    @Schema(description="Select chargeDefinitionId OR provide name/description/amount for inline ONE_TIME creation. Inline policy is AFTER_DUE_DATE. Billing required is independent from requiredForParticipation.")
    public record Charge(Long chargeDefinitionId, String name, String description, BigDecimal amount,
        boolean billingRequired, boolean requiredForParticipation,
        @Schema(description="Participation deadline, defaults to startsAt; distinct from financial dueDate.") Instant participationCutoff) {}
}
