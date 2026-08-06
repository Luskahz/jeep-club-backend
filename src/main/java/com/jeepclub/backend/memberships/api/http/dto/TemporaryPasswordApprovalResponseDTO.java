package com.jeepclub.backend.memberships.api.http.dto;

import com.jeepclub.backend.memberships.core.port.PendingFirstAccessUser;

public record TemporaryPasswordApprovalResponseDTO(
        Long userId,
        String temporaryPassword
) {

    public static TemporaryPasswordApprovalResponseDTO from(PendingFirstAccessUser result) {
        return new TemporaryPasswordApprovalResponseDTO(
                result.userId(),
                result.temporaryPassword()
        );
    }
}
