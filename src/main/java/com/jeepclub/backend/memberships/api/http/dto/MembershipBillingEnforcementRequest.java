package com.jeepclub.backend.memberships.api.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record MembershipBillingEnforcementRequest(
        @NotNull
        @Schema(description = "Novo estado da exigência financeira.", example = "false")
        Boolean enforcementEnabled
) {
}
