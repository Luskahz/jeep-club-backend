package com.jeepclub.backend.iam.identity.api.http.dto.admin.user;

import com.jeepclub.backend.iam.identity.api.module.UserActivationMethod;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Criação por convite. SENT significa aceitação pelo transporte SMTP, não entrega ao destinatário.")
public record EmailInvitationUserCreatedDTO(
        @Schema(example = "456", requiredMode = Schema.RequiredMode.REQUIRED) Long userId,
        @Schema(implementation = String.class, allowableValues = "EMAIL_INVITATION", example = "EMAIL_INVITATION", requiredMode = Schema.RequiredMode.REQUIRED) UserActivationMethod activationMethod,
        @Schema(allowableValues = {"QUEUED", "SENT", "DELIVERY_FAILED"}, example = "SENT", requiredMode = Schema.RequiredMode.REQUIRED) String invitationStatus
) implements AdminCreateUserResponseDTO {
}
