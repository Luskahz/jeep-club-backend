package com.jeepclub.backend.memberships.api.http.dto;

import com.jeepclub.backend.memberships.core.application.result.EnsureMembershipRequestResult;
import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Confirmação pública da solicitação, sem dados pessoais persistidos.")
public record MembershipApplicationSubmissionResponseDTO(
        @Schema(description = "Identificador da solicitação.", example = "123")
        Long id,
        @Schema(description = "Estado atual da solicitação.", example = "PENDING")
        MembershipApplicationStatus status,
        @Schema(description = "Indica se uma nova solicitação foi criada nesta chamada.")
        boolean created
) {
    public static MembershipApplicationSubmissionResponseDTO from(
            EnsureMembershipRequestResult result
    ) {
        return new MembershipApplicationSubmissionResponseDTO(
                result.application().getId(),
                result.application().getStatus(),
                result.created()
        );
    }
}
