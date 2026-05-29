package com.jeepclub.backend.billing.api.dto.charge;

import com.jeepclub.backend.billing.core.application.result.RefreshMemberChargeStatusesResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

@Schema(description = "Resultado do processamento de atualização de status das cobranças abertas.")
public record RefreshMemberChargeStatusesResponse(

        @Schema(description = "Quantidade de cobranças abertas analisadas.", example = "120")
        int processedCharges,

        @Schema(description = "Quantidade de cobranças marcadas como vencidas.", example = "35")
        int markedOverdueCharges,

        @Schema(description = "Quantidade de cobranças expiradas.", example = "8")
        int expiredCharges,

        @Schema(description = "Quantidade de cobranças que permaneceram sem alteração.", example = "77")
        int unchangedCharges
) {

    public static RefreshMemberChargeStatusesResponse from(
            RefreshMemberChargeStatusesResult result
    ) {
        Objects.requireNonNull(result, "result cannot be null");

        return new RefreshMemberChargeStatusesResponse(
                result.processedCharges(),
                result.markedOverdueCharges(),
                result.expiredCharges(),
                result.unchangedCharges()
        );
    }
}