package com.jeepclub.backend.iam.identity.api.http.dto.admin.user;

import com.jeepclub.backend.iam.identity.api.module.UserActivationMethod;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Criação por convite. SENT significa aceitação pelo transporte SMTP, não entrega ao destinatário.")
public record EmailInvitationUserCreatedDTO(
        @Schema(example = "456") Long userId,
        @Schema(allowableValues = "EMAIL_INVITATION", example = "EMAIL_INVITATION") UserActivationMethod activationMethod,
        @Schema(allowableValues = {"QUEUED", "SENT", "DELIVERY_FAILED"}, example = "SENT") String invitationStatus
) implements AdminCreateUserResponseDTO {
}
