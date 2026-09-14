package com.jeepclub.backend.vehicles.api.http.dto.list;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * OpenAPI-only description of the direct Spring Data page returned by vehicle listings.
 * The endpoints continue to return {@code Page<ListResponseDTO>}.
 */
@Schema(
        name = "PageVehicleListResponse",
        description = "Página Spring Data de resumos de veículos ativos."
)
public record VehiclePageResponseSchema(
        @Schema(description = "Quantidade total de veículos elegíveis.", example = "42")
        long totalElements,
        @Schema(description = "Quantidade total de páginas.", example = "5")
        int totalPages,
        @Schema(description = "Tamanho solicitado da página.", example = "10")
        int size,
        @Schema(description = "Veículos presentes na página.")
        List<ListResponseDTO> content,
        @Schema(description = "Índice zero-based da página retornada.", example = "0")
        int number,
        @Schema(description = "Metadados de ordenação retornados pelo Spring Data.")
        Map<String, Object> sort,
        @Schema(description = "Metadados de paginação retornados pelo Spring Data.")
        Map<String, Object> pageable,
        @Schema(description = "Quantidade de elementos nesta página.", example = "10")
        int numberOfElements,
        @Schema(description = "Indica se esta é a primeira página.", example = "true")
        boolean first,
        @Schema(description = "Indica se esta é a última página.", example = "false")
        boolean last,
        @Schema(description = "Indica se a página está vazia.", example = "false")
        boolean empty
) {
}
