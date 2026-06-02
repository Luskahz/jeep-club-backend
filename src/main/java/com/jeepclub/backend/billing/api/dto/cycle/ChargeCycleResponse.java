package com.jeepclub.backend.billing.api.dto.cycle;

import com.jeepclub.backend.billing.core.application.result.cycle.ChargeCycleResult;
import com.jeepclub.backend.billing.core.domain.enums.cycle.ChargeCycleStatus;
import com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType;
import com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Schema(description = "Resposta com dados de um ciclo de cobrança.")
public record ChargeCycleResponse(

        @Schema(description = "Identificador do ciclo.", example = "1")
        Long id,

        @Schema(description = "Identificador da definição de cobrança.", example = "10")
        Long chargeDefinitionId,

        @Schema(description = "Nome da definição de cobrança no momento em que o ciclo foi gerado.", example = "Anuidade Pesca 2026")
        String chargeDefinitionNameSnapshot,

        @Schema(description = "Descrição da definição de cobrança no momento em que o ciclo foi gerado.", example = "Cobrança anual para membros participantes da pesca.", nullable = true)
        String chargeDefinitionDescriptionSnapshot,

        @Schema(description = "Valor padrão da definição de cobrança no momento em que o ciclo foi gerado.", example = "250.00")
        BigDecimal chargeDefinitionDefaultAmountSnapshot,

        @Schema(description = "Tipo de recorrência da definição de cobrança no momento em que o ciclo foi gerado.", example = "YEARLY")
        ChargeRecurrenceType chargeDefinitionRecurrenceTypeSnapshot,

        @Schema(description = "Obrigatoriedade da definição de cobrança no momento em que o ciclo foi gerado.", example = "true")
        Boolean chargeDefinitionRequiredSnapshot,

        @Schema(description = "Política de aceitação de pagamento da definição no momento em que o ciclo foi gerado.", example = "AFTER_DUE_DATE")
        PaymentAcceptancePolicy chargeDefinitionPaymentAcceptancePolicySnapshot,

        @Schema(description = "Dias de tolerância para pagamento após vencimento no momento em que o ciclo foi gerado.", example = "15", nullable = true)
        Integer chargeDefinitionLatePaymentGraceDaysSnapshot,

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

        @Schema(description = "Usuário que cancelou o ciclo.", example = "1", nullable = true)
        Long canceledByUserId,

        @Schema(description = "Data e hora em que o ciclo foi finalizado.", nullable = true)
        Instant finishedAt,

        @Schema(description = "Usuário que finalizou o ciclo.", example = "1", nullable = true)
        Long finishedByUserId,

        @Schema(description = "Data e hora em que o ciclo foi arquivado.", nullable = true)
        Instant archivedAt,

        @Schema(description = "Usuário que arquivou o ciclo.", example = "1", nullable = true)
        Long archivedByUserId,

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
                result.chargeDefinitionNameSnapshot(),
                result.chargeDefinitionDescriptionSnapshot(),
                result.chargeDefinitionDefaultAmountSnapshot(),
                result.chargeDefinitionRecurrenceTypeSnapshot(),
                result.chargeDefinitionRequiredSnapshot(),
                result.chargeDefinitionPaymentAcceptancePolicySnapshot(),
                result.chargeDefinitionLatePaymentGraceDaysSnapshot(),
                result.code(),
                result.dueDate(),
                result.status(),
                result.generatedByUserId(),
                result.generatedAt(),
                result.canceledAt(),
                result.canceledByUserId(),
                result.finishedAt(),
                result.finishedByUserId(),
                result.archivedAt(),
                result.archivedByUserId(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}