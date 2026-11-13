package com.jeepclub.backend.dependents.api.http.dto.dependent;

import com.jeepclub.backend.dependents.core.application.query.AdminDependentField;
import com.jeepclub.backend.dependents.core.application.result.AdminDependentSearchResult;
import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Schema(description = "Dados administrativos de um dependente com identificação do titular.")
public record AdminDependentResponseDTO(
        @Schema(description = "Identificador único do dependente.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Nome completo do dependente.", example = "Maria Silva", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "CPF do dependente.", example = "98765432109", requiredMode = Schema.RequiredMode.REQUIRED)
        String cpf,

        @Schema(description = "Data de nascimento do dependente.", example = "2015-08-25", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate birthDate,

        @Schema(description = "Tipo de relacionamento com o usuário titular.", example = "CHILD", requiredMode = Schema.RequiredMode.REQUIRED)
        RelationshipType relationshipType,

        @Schema(description = "Telefone de contato do dependente.", example = "11988887777", nullable = true)
        String phoneNumber,

        @Schema(description = "Identificador do usuário titular.", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        Long userId,

        @Schema(description = "Nome completo do usuário titular.", example = "João Silva", nullable = true)
        String ownerName,

        @Schema(description = "Status atual do dependente.", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "ARCHIVED"}, requiredMode = Schema.RequiredMode.REQUIRED)
        DependentStatus status,

        @Schema(description = "Data de criação do dependente.", example = "2026-05-22T21:30:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,

        @Schema(description = "Data da última atualização do dependente.", example = "2026-05-23T14:20:00Z", nullable = true)
        Instant updatedAt,

        @Schema(description = "Data de arquivamento do dependente.", example = "2026-05-24T10:00:00Z", nullable = true)
        Instant archivedAt,

        @Schema(description = "Rota protegida de leitura administrativa da foto; não é chave de storage nem URL física.", nullable = true, example = "/users/5/dependents/1/photo")
        String photoUrl
) {

    public static AdminDependentResponseDTO from(AdminDependentSearchResult row) {
        Objects.requireNonNull(row, "AdminDependentSearchResult cannot be null");
        return new AdminDependentResponseDTO(
                row.id(),
                row.name(),
                row.cpf(),
                row.birthDate(),
                row.relationshipType(),
                row.phoneNumber(),
                row.userId(),
                row.ownerName(),
                row.status(),
                row.createdAt(),
                row.updatedAt(),
                row.archivedAt(),
                row.photoUrl()
        );
    }

    public static Map<String, Object> selected(
            AdminDependentSearchResult row,
            Set<AdminDependentField> fields
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (AdminDependentField field : fields) {
            result.put(field.property(), switch (field) {
                case ID -> row.id();
                case NAME -> row.name();
                case CPF -> row.cpf();
                case BIRTH_DATE -> row.birthDate();
                case RELATIONSHIP_TYPE -> row.relationshipType();
                case PHONE_NUMBER -> row.phoneNumber();
                case USER_ID -> row.userId();
                case OWNER_NAME -> row.ownerName();
                case STATUS -> row.status();
                case CREATED_AT -> row.createdAt();
                case UPDATED_AT -> row.updatedAt();
                case ARCHIVED_AT -> row.archivedAt();
                case PHOTO_URL -> row.photoUrl();
            });
        }
        return result;
    }
}
