package com.jeepclub.backend.iam.identity.api.http.dto.admin.user;

import com.jeepclub.backend.iam.identity.api.http.dto.user.UpdateCurrentUserProfileRequestDTO;
import com.jeepclub.backend.iam.identity.core.application.command.AdminUserUpdate;
import com.jeepclub.backend.iam.identity.core.application.command.ProfileUpdate.Field;
import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import com.jeepclub.backend.iam.identity.core.domain.model.User;

@Schema(name = "AdminUpdateUserRequest", additionalProperties = Schema.AdditionalPropertiesValue.FALSE,
        description = "PATCH parcial: omitir preserva; null limpa opcionais. name e cpf não aceitam null/branco. "
                + "{} não altera updatedAt. IDs, status, roles, credenciais, timestamps e campos desconhecidos são rejeitados. "
                + "CPF/e-mail alterados revogam sessões/challenges e invalidam links pendentes afetados; reenviar convite separadamente. "
                + "Outro administrador não pode alterar CPF/e-mail de ROOT.")
public record AdminUpdateUserRequestDTO(
        @Schema(description = "Nome aparado, não branco quando presente.", maxLength = 150) String name,
        @Schema(description = "Nascimento ISO yyyy-MM-dd. null remove.", format = "date", nullable = true) LocalDate birthDate,
        @Email @Size(max = 180) @Schema(description = "E-mail válido aparado e em minúsculas; null/branco removem.", nullable = true, maxLength = 180) String email,
        @Schema(description = "CPF válido, 11 dígitos ou 000.000.000-00; único. null é inválido.", example = "529.982.247-25") String cpf,
        @Schema(description = "RG único, até 20 dígitos após normalização. null/branco removem.", nullable = true) String rg,
        @Schema(description = "Telefone até 20 dígitos após normalização. null/branco removem.", nullable = true) String phoneNumber,
        @Schema(description = "Chave images/... existente de /media/images. null remove; branco/URL inválidos.", nullable = true, maxLength = 255) String profilePhotoStorageKey) {

    public static AdminUserUpdate read(JsonNode body, Validator validator) {
        if (body == null || !body.isObject()) throw new IllegalArgumentException("User update must be a JSON object.");
        Field<String> cpf = text(body, "cpf");
        Field<String> email = text(body, "email");
        if (email.present() && !validator.validateValue(AdminUpdateUserRequestDTO.class, "email", User.normalizeEmail(email.value())).isEmpty()) {
            throw new IllegalArgumentException("Invalid email.");
        }
        ObjectNode profile = ((ObjectNode) body).deepCopy();
        profile.remove("cpf");
        profile.remove("email");
        return new AdminUserUpdate(UpdateCurrentUserProfileRequestDTO.read(profile), cpf, email);
    }

    private static Field<String> text(JsonNode body, String name) {
        JsonNode value = body.get(name);
        if (value == null) return Field.omitted();
        if (value.isNull()) return Field.provided(null);
        if (!value.isString()) throw new IllegalArgumentException("User field must be a string or null.");
        return Field.provided(value.asString());
    }
}
