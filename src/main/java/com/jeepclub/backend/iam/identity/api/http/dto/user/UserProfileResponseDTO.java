package com.jeepclub.backend.iam.identity.api.http.dto.user;

import com.jeepclub.backend.iam.identity.core.domain.model.UserProfile;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "IdentityUserProfileResponse")
public record UserProfileResponseDTO(
        @Schema(description = "Dados profissionais atuais, ou null quando ausentes.", nullable = true) UserProfileDataDTO.Work workProfile,
        @Schema(description = "Endereço residencial atual, ou null quando ausente.", nullable = true) UserProfileDataDTO.Address address,
        @Schema(description = "Derivada dos dados. true enquanto ocupação, local de trabalho ou endereço estiverem incompletos. "
                + "Informativa; não bloqueia login ou onboarding.", requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_ONLY) boolean profileCompletionPending) {
    public static UserProfileResponseDTO from(UserProfile profile) {
        var work = profile.workProfile();
        var address = profile.address();
        return new UserProfileResponseDTO(work == null ? null : new UserProfileDataDTO.Work(work.occupation(), work.workplace()),
                address == null ? null : new UserProfileDataDTO.Address(address.postalCode(), address.street(), address.number(),
                        address.complement(), address.neighborhood(), address.city(), address.state()), profile.profileCompletionPending());
    }
}
