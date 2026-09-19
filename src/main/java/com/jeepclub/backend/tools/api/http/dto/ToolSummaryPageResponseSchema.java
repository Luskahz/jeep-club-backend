package com.jeepclub.backend.tools.api.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "PageResponseToolSummaryResponseDTO", description = "Página estável de ferramentas do membro.")
public record ToolSummaryPageResponseSchema(
        List<ToolSummaryResponseDTO> content,
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
