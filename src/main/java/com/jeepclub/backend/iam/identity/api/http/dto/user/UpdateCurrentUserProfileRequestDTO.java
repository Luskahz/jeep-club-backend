package com.jeepclub.backend.iam.identity.api.http.dto.user;

import com.jeepclub.backend.iam.identity.core.application.command.ProfileUpdate;
import com.jeepclub.backend.iam.identity.core.application.command.ProfileUpdate.Field;
import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

@Schema(name = "UpdateCurrentUserProfileRequest", additionalProperties = Schema.AdditionalPropertiesValue.FALSE,
        description = "Edição parcial do cadastro próprio. Omitir preserva; null limpa campos opcionais. "
                + "name não aceita null/branco. Objeto vazio não altera dados nem updatedAt. "
                + "CPF, id/userId, status, roles, permissions e timestamps são proibidos, assim como campos desconhecidos. "
                + "E-mail usa exclusivamente PATCH /identity/me/email. Nenhuma URL de foto é aceita.")
public record UpdateCurrentUserProfileRequestDTO(
        @Schema(description = "Nome aparado, obrigatório quando enviado; null/branco são inválidos.",
                maxLength = 150, example = "Maria da Silva") String name,
        @Schema(description = "Nascimento em ISO yyyy-MM-dd; null remove. Omitir preserva.",
                format = "date", nullable = true, example = "2000-05-17") LocalDate birthDate,
        @Schema(description = "RG único; pontuação removida, até 20 dígitos canônicos. null/branco removem.",
                nullable = true, example = "12.345.678-9") String rg,
        @Schema(description = "Telefone normalizado para até 20 dígitos. null/branco removem.",
                nullable = true, example = "+55 (11) 99999-9999") String phoneNumber,
        @Schema(description = "Chave de imagem existente retornada por POST /media/images. null remove a associação "
                + "sem apagar o arquivo compartilhável. URL arbitrária e branco são inválidos.",
                nullable = true, maxLength = 255,
                example = "images/2026/09/24/550e8400-e29b-41d4-a716-446655440000.png") String profilePhotoStorageKey
) {
    private static final Set<String> ALLOWED = Set.of("name", "birthDate", "rg", "phoneNumber", "profilePhotoStorageKey");

    public static ProfileUpdate read(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw new IllegalArgumentException("Profile update must be a JSON object.");
        }
        for (String field : body.propertyNames()) {
            if (!ALLOWED.contains(field)) {
                throw new IllegalArgumentException("Unsupported profile field.");
            }
        }
        Field<String> date = text(body, "birthDate");
        Field<LocalDate> birthDate;
        try {
            birthDate = date.present()
                    ? Field.provided(date.value() == null ? null : LocalDate.parse(date.value()))
                    : Field.omitted();
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("birthDate must be an ISO date.", exception);
        }
        return new ProfileUpdate(text(body, "name"), birthDate, text(body, "rg"),
                text(body, "phoneNumber"), text(body, "profilePhotoStorageKey"));
    }

    private static Field<String> text(JsonNode body, String name) {
        JsonNode value = body.get(name);
        if (value == null) return Field.omitted();
        if (value.isNull()) return Field.provided(null);
        if (!value.isString()) throw new IllegalArgumentException("Profile field must be a string or null.");
        return Field.provided(value.asString());
    }
}
