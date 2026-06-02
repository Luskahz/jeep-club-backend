package com.jeepclub.backend.dependents.api.dto.dependent;

import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Schema(description = "Detalhamento completo de um dependente cadastrado no Jeep Club.")
public record DependentResponseDTO(
        @Schema(description = "Identificador único do dependente.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Nome completo do dependente.", example = "Mariana Silva", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "CPF do dependente.", example = "98765432109")
        String cpf,

        @Schema(description = "Data de nascimento do dependente.", example = "2015-08-25")
        LocalDate birthDate,

        @Schema(description = "Tipo de relacionamento.", example = "CHILD", requiredMode = Schema.RequiredMode.REQUIRED)
        String relationshipType,

        @Schema(description = "Telefone de contato.", example = "11988887777")
        String phoneNumber,

        @Schema(description = "Perfil médico do dependente.")
        MedicalProfileDTO medicalProfile,

        @Schema(description = "Indica se o termo de consentimento LGPD está aceito.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean consentAccepted,

        @Schema(description = "Data/hora em que o termo de consentimento foi aceito.", example = "2026-05-22T21:30:00Z")
        Instant consentAcceptedAt,

        @Schema(description = "Identificador do Sócio titular dono deste dependente.", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        Long socioId,

        @Schema(description = "Data de criação do registro.", example = "2026-05-22T21:30:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,

        @Schema(description = "Data da última atualização.", example = "2026-05-22T21:30:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant updatedAt
) {
    public static DependentResponseDTO from(Dependent domain) {
        Objects.requireNonNull(domain, "Domain model Dependent cannot be null");

        return new DependentResponseDTO(
                domain.getId(),
                domain.getName(),
                domain.getCpf(),
                domain.getBirthDate(),
                domain.getRelationshipType().name(),
                domain.getPhoneNumber(),
                MedicalProfileDTO.from(domain.getMedicalProfile()),
                domain.isConsentAccepted(),
                domain.getConsentAcceptedAt(),
                domain.getSocioId(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public static List<DependentResponseDTO> from(List<Dependent> list) {
        Objects.requireNonNull(list, "Dependents list cannot be null");
        return list.stream()
                .map(DependentResponseDTO::from)
                .toList();
    }
}

