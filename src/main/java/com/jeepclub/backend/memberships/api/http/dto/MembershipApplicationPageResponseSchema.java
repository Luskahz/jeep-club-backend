package com.jeepclub.backend.memberships.api.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "PageResponseMembershipApplicationResponseDTO", description = "Página estável de solicitações de adesão.")
public record MembershipApplicationPageResponseSchema(
        List<MembershipApplicationResponseDTO> content,
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
