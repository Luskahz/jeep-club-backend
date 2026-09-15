package com.jeepclub.backend.memberships.api.http.dto;

import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Representação pública de uma solicitação de adesão.")
public record MembershipApplicationResponseDTO(
        @Schema(description = "Identificador da solicitação.", example = "123")
        Long id,
        @Schema(description = "Nome informado pelo candidato.", example = "Marina da Silva")
        String name,
        @Schema(description = "CPF normalizado com 11 dígitos.", example = "12345678909")
        String cpf,
        @Schema(description = "E-mail normalizado do candidato.", example = "marina.silva@example.com", nullable = true)
        String email,
        @Schema(description = "Telefone normalizado, apenas dígitos.", example = "11912345678")
        String phoneNumber,
        @Schema(description = "Mensagem opcional enviada pelo candidato.", nullable = true)
        String message,
        @Schema(description = "Estado atual da solicitação.", example = "PENDING")
        MembershipApplicationStatus status,
        @Schema(description = "Motivo registrado na rejeição, quando houver.", nullable = true)
        String rejectionReason,
        @Schema(description = "Instante de criação da solicitação.", format = "date-time")
        Instant requestedAt,
        @Schema(description = "Último instante de alteração da solicitação.", format = "date-time")
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
