package com.jeepclub.backend.membershipKauan.api.dto.membership;

import com.jeepclub.backend.membershipKauan.core.domain.model.MembershipApplication;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Schema(description = "Dados de uma solicitação de associação ao Jeep Club.")
public record MembershipApplicationResponseDTO(

        @Schema(
                description = "Identificador único da solicitação de associação.",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long id,

        @Schema(
                description = "Nome completo do solicitante.",
                example = "Lucas Alves",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String name,

        @Schema(
                description = "CPF do solicitante.",
                example = "12345678909",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String cpf,

        @Schema(
                description = "E-mail de contato do solicitante.",
                example = "lucas.alves@email.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String email,

        @Schema(
                description = "Telefone de contato do solicitante.",
                example = "+5512999999999",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String phoneNumber,

        @Schema(
                description = "Mensagem enviada pelo solicitante.",
                example = "Tenho interesse em participar do Jeep Club e gostaria de receber mais informações.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String message,

        @Schema(
                description = "Status atual da solicitação de associação.",
                example = "PENDING",
                allowableValues = {"PENDING", "APPROVED", "REJECTED"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String status,

        @Schema(
                description = "Data de criação da solicitação.",
                example = "2026-05-17T20:30:00Z",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Instant createdAt
) {
    public static MembershipApplicationResponseDTO from(MembershipApplication application) {
        Objects.requireNonNull(application, "Membership application cannot be null");

        return new MembershipApplicationResponseDTO(
                application.getId(),
                application.getName(),
                application.getCpf(),
                application.getEmail(),
                application.getPhoneNumber(),
                application.getMessage(),
                application.getStatus().name(),
                application.getCreatedAt()
        );
    }

    public static List<MembershipApplicationResponseDTO> from(List<MembershipApplication> applications) {
        Objects.requireNonNull(applications, "Membership applications cannot be null");

        return applications.stream()
                .map(MembershipApplicationResponseDTO::from)
                .toList();
    }
}