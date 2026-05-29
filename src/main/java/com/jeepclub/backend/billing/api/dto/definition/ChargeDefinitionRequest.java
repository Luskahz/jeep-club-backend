package com.jeepclub.backend.billing.api.dto.definition;

import com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType;
import com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Dados para criação de uma definição de cobrança.")
public record ChargeDefinitionRequest(

        @NotBlank(message = "Nome da cobrança é obrigatório.")
        @Size(max = 120, message = "Nome da cobrança deve ter no máximo 120 caracteres.")
        @Schema(
                description = "Nome da cobrança.",
                example = "Anuidade",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String name,

        @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres.")
        @Schema(
                description = "Descrição da cobrança.",
                example = "Cobrança anual obrigatória dos membros do clube.",
                nullable = true
        )
        String description,

        @NotNull(message = "Valor padrão é obrigatório.")
        @DecimalMin(value = "0.01", message = "Valor padrão deve ser maior que zero.")
        @Schema(
                description = "Valor padrão da cobrança.",
                example = "250.00",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        BigDecimal defaultAmount,

        @NotNull(message = "Tipo de recorrência é obrigatório.")
        @Schema(
                description = "Tipo de recorrência da cobrança.",
                example = "YEARLY",
                allowableValues = {"ONE_TIME", "MONTHLY", "YEARLY"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        ChargeRecurrenceType recurrenceType,

        @NotNull(message = "Obrigatoriedade da cobrança é obrigatória.")
        @Schema(
                description = "Define se a cobrança é obrigatória para o público-alvo.",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Boolean required,

        @NotNull(message = "Política de aceitação de pagamento é obrigatória.")
        @Schema(
                description = "Define até quando cobranças geradas por esta definição aceitam pagamento.",
                example = "AFTER_DUE_DATE",
                allowableValues = {
                        "UNTIL_DUE_DATE",
                        "AFTER_DUE_DATE",
                        "UNTIL_DAYS_AFTER_DUE_DATE"
                },
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        PaymentAcceptancePolicy paymentAcceptancePolicy,

        @Positive(message = "Dias de tolerância para pagamento atrasado deve ser maior que zero.")
        @Schema(
                description = "Quantidade de dias após o vencimento em que o pagamento ainda será aceito. Obrigatório somente para UNTIL_DAYS_AFTER_DUE_DATE.",
                example = "15",
                nullable = true
        )
        Integer latePaymentGraceDays
) {
}