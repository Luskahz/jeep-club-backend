package com.jeepclub.backend.iam.authorization.api.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Autoridades efetivas do usuário autenticado.")
public record CurrentAuthorizationResponseDTO(
        @Schema(description = "ID estável do usuário.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long userId,
        @Schema(description = "Códigos das authorities efetivas, em ordem alfabética.",
                example = "[\"IDENTITY_USER_READ\"]", requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> authorities
) {
    public CurrentAuthorizationResponseDTO {
        authorities = List.copyOf(authorities);
    }
}
