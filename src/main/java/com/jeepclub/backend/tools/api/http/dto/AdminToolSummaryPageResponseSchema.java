package com.jeepclub.backend.tools.api.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * OpenAPI-only description of the direct Spring Data page returned to admins.
 * The endpoint continues to return {@code Page<AdminToolSummaryResponseDTO>}.
 */
@Schema(
        name = "PageAdminToolSummaryResponse",
        description = "Página Spring Data de ferramentas para consulta administrativa."
)
public record AdminToolSummaryPageResponseSchema(
        @Schema(description = "Quantidade total de ferramentas elegíveis.", example = "42")
        long totalElements,
        @Schema(description = "Quantidade total de páginas.", example = "3")
        int totalPages,
        @Schema(description = "Tamanho solicitado da página.", example = "20")
        int size,
        @Schema(description = "Ferramentas presentes na página.")
        List<AdminToolSummaryResponseDTO> content,
        @Schema(description = "Índice zero-based da página retornada.", example = "0")
        int number,
        @Schema(description = "Metadados de ordenação retornados pelo Spring Data.")
        Map<String, Object> sort,
        @Schema(description = "Metadados de paginação retornados pelo Spring Data.")
        Map<String, Object> pageable,
        @Schema(description = "Quantidade de elementos nesta página.", example = "20")
        int numberOfElements,
        @Schema(description = "Indica se esta é a primeira página.", example = "true")
        boolean first,
        @Schema(description = "Indica se esta é a última página.", example = "false")
        boolean last,
        @Schema(description = "Indica se a página está vazia.", example = "false")
        boolean empty
) {
}
