package com.jeepclub.backend.iam.authentication.api.http.dto.activation;

public record ActivationInvitationStatusDTO(
        @io.swagger.v3.oas.annotations.media.Schema(example = "456") Long userId,
        @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {"QUEUED", "SENT", "DELIVERY_FAILED"},
                example = "SENT", description = "SENT indica aceitação pelo SMTP, sem garantia de entrega.")
        String invitationStatus) {
}
