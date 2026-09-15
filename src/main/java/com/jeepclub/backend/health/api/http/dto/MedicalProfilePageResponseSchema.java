package com.jeepclub.backend.health.api.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * OpenAPI-only description of the Spring Data page returned by the administrative list.
 * The endpoint continues to return {@code Page<MedicalProfileSummaryResponse>}.
 */
@Schema(
        name = "PageMedicalProfileSummaryResponse",
        description = "Página Spring Data de resumos administrativos de perfis médicos."
)
public record MedicalProfilePageResponseSchema(
        @Schema(description = "Quantidade total de elementos elegíveis.", example = "42")
        long totalElements,
        @Schema(description = "Quantidade total de páginas.", example = "3")
        int totalPages,
        @Schema(description = "Tamanho solicitado da página.", example = "20")
        int size,
        @Schema(description = "Perfis presentes na página.")
        List<MedicalProfileSummaryResponse> content,
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
