package com.jeepclub.backend.memberships.api.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MembershipBillingConfigurationRequest(
        @NotNull
        @Positive
        @Schema(description = "Identificador da definição de cobrança ativa em Billing.", example = "12")
        Long chargeDefinitionId,
        @NotNull
        @Schema(description = "Indica se a validação financeira está habilitada.", example = "true")
        Boolean enforcementEnabled
) {
}
