package com.jeepclub.backend.authentication.api.dto.membership;

import com.jeepclub.backend.authentication.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.authentication.core.domain.model.MembershipApplication;

import java.time.Instant;

public record MembershipApplicationResponseDTO(
        Long id,
        String name,
        String cpf,
        String email,
        String phoneNumber,
        String message,
        MembershipApplicationStatus status,
        Instant createdAt
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
                application.getCreatedAt()
        );
    }
}