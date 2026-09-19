package com.jeepclub.backend.health.api.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "PageResponseMedicalProfileSummaryResponse", description = "Página estável de resumos de perfis médicos.")
public record MedicalProfilePageResponseSchema(
        List<MedicalProfileSummaryResponse> content,
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
