package com.jeepclub.backend.billing.api.dto.cycle;

import com.jeepclub.backend.billing.core.application.result.cycle.ChargeCycleResult;
import com.jeepclub.backend.billing.core.domain.enums.ChargeCycleStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Schema(description = "Resposta com dados de um ciclo de cobrança.")
public record ChargeCycleResponse(

        @Schema(description = "Identificador do ciclo.", example = "1")
        Long id,

        @Schema(description = "Identificador da definição de cobrança.", example = "10")
        Long chargeDefinitionId,

        @Schema(description = "Código do ciclo.", example = "2026")
        String code,

        @Schema(description = "Data de vencimento das cobranças do ciclo.", example = "2026-02-10")
        LocalDate dueDate,

        @Schema(description = "Status do ciclo.", example = "GENERATED")
        ChargeCycleStatus status,

        @Schema(description = "Usuário que gerou o ciclo, quando gerado manualmente.", example = "1", nullable = true)
        Long generatedByUserId,

        @Schema(description = "Data e hora em que o ciclo foi gerado.")
        Instant generatedAt,

        @Schema(description = "Data e hora em que o ciclo foi cancelado.", nullable = true)
        Instant canceledAt,

        @Schema(description = "Data de criação do registro.")
        Instant createdAt,

        @Schema(description = "Data da última atualização do registro.", nullable = true)
        Instant updatedAt
) {

    public static ChargeCycleResponse from(ChargeCycleResult result) {
        Objects.requireNonNull(result, "result cannot be null");

        return new ChargeCycleResponse(
                result.id(),
                result.chargeDefinitionId(),
                result.code(),
                result.dueDate(),
                result.status(),
                result.generatedByUserId(),
                result.generatedAt(),
                result.canceledAt(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}