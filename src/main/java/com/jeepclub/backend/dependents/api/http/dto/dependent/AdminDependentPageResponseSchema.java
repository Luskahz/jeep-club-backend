package com.jeepclub.backend.dependents.api.http.dto.dependent;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "PageResponseAdminDependentResponseDTO", description = "Página estável de dependentes para consulta administrativa.")
public record AdminDependentPageResponseSchema(
        List<AdminDependentResponseDTO> content,
        int number,
        int size,
        long totalElements,
        int totalPages,
        int numberOfElements,
        boolean first,
        boolean last,
        boolean empty
) {
}
