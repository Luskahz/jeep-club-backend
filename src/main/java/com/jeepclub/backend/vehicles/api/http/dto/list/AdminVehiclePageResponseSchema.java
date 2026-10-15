package com.jeepclub.backend.vehicles.api.http.dto.list;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "PageResponseAdminListResponseDTO", description = "Página estável de resumos de veículos.")
public record AdminVehiclePageResponseSchema(
        List<AdminListResponseDTO> content,
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
