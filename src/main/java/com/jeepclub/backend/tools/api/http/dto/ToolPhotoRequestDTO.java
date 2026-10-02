package com.jeepclub.backend.tools.api.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ToolPhotoRequestDTO(
        @Schema(description = "Chave retornada por POST /media/images. null remove a associação, preservando o objeto no storage.", nullable = true,
                example = "images/2026/09/24/550e8400-e29b-41d4-a716-446655440000.png")
        String storageKey
) {}
