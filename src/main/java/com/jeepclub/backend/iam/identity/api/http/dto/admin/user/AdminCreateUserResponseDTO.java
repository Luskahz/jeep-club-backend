package com.jeepclub.backend.iam.identity.api.http.dto.admin.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jeepclub.backend.iam.identity.api.module.AdminUserCreationResult;
import com.jeepclub.backend.iam.identity.api.module.UserActivationMethod;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Senha provisória somente em TEMPORARY_PASSWORD; status QUEUED somente em EMAIL_INVITATION.")
public record AdminCreateUserResponseDTO(Long userId, UserActivationMethod activationMethod,
                                         String temporaryPassword, String invitationStatus) {
    public static AdminCreateUserResponseDTO from(AdminUserCreationResult result) {
        return new AdminCreateUserResponseDTO(result.userId(), result.activationMethod(),
                result.temporaryPassword(), result.invitationStatus());
    }
}
