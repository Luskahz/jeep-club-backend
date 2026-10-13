package com.jeepclub.backend.iam.identity.api.http.dto.admin.user;

import com.jeepclub.backend.iam.identity.api.module.AdminUserCreationResult;
import com.jeepclub.backend.iam.identity.api.module.UserActivationMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;

@Schema(oneOf = {TemporaryPasswordUserCreatedDTO.class, EmailInvitationUserCreatedDTO.class},
        discriminatorProperty = "activationMethod",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "TEMPORARY_PASSWORD", schema = TemporaryPasswordUserCreatedDTO.class),
                @DiscriminatorMapping(value = "EMAIL_INVITATION", schema = EmailInvitationUserCreatedDTO.class)
        },
        description = "Resposta discriminada por activationMethod.")
public sealed interface AdminCreateUserResponseDTO
        permits TemporaryPasswordUserCreatedDTO, EmailInvitationUserCreatedDTO {
    static AdminCreateUserResponseDTO from(AdminUserCreationResult result, String invitationStatus) {
        if (result.activationMethod() == UserActivationMethod.TEMPORARY_PASSWORD) {
            return new TemporaryPasswordUserCreatedDTO(result.userId(), result.activationMethod(),
                    result.temporaryPassword());
        }
        return new EmailInvitationUserCreatedDTO(result.userId(), result.activationMethod(), invitationStatus);
    }
}
