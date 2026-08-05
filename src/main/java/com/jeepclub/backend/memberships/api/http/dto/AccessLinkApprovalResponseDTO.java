package com.jeepclub.backend.memberships.api.http.dto;

import com.jeepclub.backend.memberships.core.port.PendingFirstAccessLink;

public record AccessLinkApprovalResponseDTO(
        Long userId,
        String accessLink
) {

    public static AccessLinkApprovalResponseDTO from(PendingFirstAccessLink result) {
        return new AccessLinkApprovalResponseDTO(
                result.userId(),
                result.accessLink()
        );
    }
}
