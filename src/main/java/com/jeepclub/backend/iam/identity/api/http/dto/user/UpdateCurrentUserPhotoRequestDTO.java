package com.jeepclub.backend.iam.identity.api.http.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateCurrentUserPhotoRequestDTO(
        @Schema(description = "Chave retornada por POST /media/images; null remove a associação sem apagar o objeto compartilhável.", nullable = true,
                example = "images/2026/09/24/550e8400-e29b-41d4-a716-446655440000.png")
        String storageKey
) {}
