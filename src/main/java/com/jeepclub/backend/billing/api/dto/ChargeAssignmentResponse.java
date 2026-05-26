package com.jeepclub.backend.billing.api.dto;

import com.jeepclub.backend.billing.core.application.result.ChargeAssignmentResult;
import com.jeepclub.backend.billing.core.domain.enums.ChargeAssignmentType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Schema(description = "Resposta com os dados de uma regra de atribuição de cobrança.")
public record ChargeAssignmentResponse(

        @Schema(description = "Identificador da atribuição.", example = "1")
        Long id,

        @Schema(description = "Identificador da definição de cobrança.", example = "10")
        Long chargeDefinitionId,

        @Schema(description = "Tipo de atribuição da cobrança.", example = "ALL_MEMBERS")
        ChargeAssignmentType assignmentType,

        @Schema(description = "Identificador do alvo da atribuição, quando aplicável.", example = "5", nullable = true)
        Long targetId,

        @Schema(description = "Indica se a atribuição está ativa.", example = "true")
        boolean active,

        @Schema(description = "Data de criação da atribuição.")
        Instant createdAt,

        @Schema(description = "Data da última atualização da atribuição.", nullable = true)
        Instant updatedAt
) {

    public static ChargeAssignmentResponse from(ChargeAssignmentResult result) {
        Objects.requireNonNull(result, "result cannot be null");

        return new ChargeAssignmentResponse(
                result.id(),
                result.chargeDefinitionId(),
                result.assignmentType(),
                result.targetId(),
                result.active(),
                result.createdAt(),
                result.updatedAt()
        );
    }

    public static List<ChargeAssignmentResponse> from(List<ChargeAssignmentResult> results) {
        Objects.requireNonNull(results, "results cannot be null");

        return results.stream()
                .map(ChargeAssignmentResponse::from)
                .toList();
    }
}