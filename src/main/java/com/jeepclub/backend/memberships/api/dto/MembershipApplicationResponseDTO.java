package com.jeepclub.backend.memberships.api.dto;

import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;

import java.time.Instant;

public record MembershipApplicationResponseDTO(
        Long id,
        String name,
        String cpf,
        String email,
        String phoneNumber,
        String message,
        MembershipApplicationStatus status,
        String rejectionReason,
        Instant requestedAt,
        Instant updatedAt
) {
    public static MembershipApplicationResponseDTO fromDomain(MembershipApplication application) {
        return new MembershipApplicationResponseDTO(
                application.getId(),
                application.getName(),
                application.getCpf(),
                application.getEmail(),
                application.getPhoneNumber(),
                application.getMessage(),
                application.getStatus(),
                application.getRejectionReason(),
                application.getRequestedAt(),
                application.getUpdatedAt()
        );
    }
}