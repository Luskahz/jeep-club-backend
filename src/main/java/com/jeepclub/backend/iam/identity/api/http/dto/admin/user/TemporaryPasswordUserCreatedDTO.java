package com.jeepclub.backend.iam.identity.api.http.dto.admin.user;

import com.jeepclub.backend.iam.identity.api.module.UserActivationMethod;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Criação com senha provisória, exibida apenas nesta resposta.")
public record TemporaryPasswordUserCreatedDTO(
        @Schema(example = "456") Long userId,
        @Schema(allowableValues = "TEMPORARY_PASSWORD", example = "TEMPORARY_PASSWORD") UserActivationMethod activationMethod,
        @Schema(description = "Segredo de uso inicial; nunca recuperável.", example = "<senha provisória>") String temporaryPassword
) implements AdminCreateUserResponseDTO {
}
