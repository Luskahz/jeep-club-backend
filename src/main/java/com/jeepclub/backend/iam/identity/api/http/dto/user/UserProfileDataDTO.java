package com.jeepclub.backend.iam.identity.api.http.dto.user;

import com.jeepclub.backend.iam.identity.core.domain.model.ResidentialAddress;
import com.jeepclub.backend.iam.identity.core.domain.model.WorkProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;
import java.util.Set;

@Schema(name = "IdentityUserProfileData", additionalProperties = Schema.AdditionalPropertiesValue.FALSE,
        description = "Substituição integral do perfil complementar. Campos omitidos, null ou textos em branco removem dados. "
                + "{} limpa ambos os blocos. Preenchimento parcial é permitido e mantém a pendência. "
                + "Textos são aparados antes da validação; limites descritos referem-se ao valor normalizado, não ao texto bruto. "
                + "IDs e campos desconhecidos são rejeitados.")
public record UserProfileDataDTO(
        @Schema(description = "Perfil profissional; null ou omissão remove o bloco.", nullable = true) Work workProfile,
        @Schema(description = "Endereço residencial; null ou omissão remove o bloco.", nullable = true) Address address) {
    @Schema(name = "IdentityWorkProfile", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
    public record Work(
            @Schema(description = "Ocupação ou atividade, necessária para completude. Até 150 caracteres após aparar; branco remove.", nullable = true) String occupation,
            @Schema(description = "Empresa, organização ou local de trabalho, necessário para completude. Até 150 caracteres após aparar; branco remove.", nullable = true) String workplace) {
        public WorkProfile toDomain() { return new WorkProfile(occupation, workplace); }
    }

    @Schema(name = "IdentityResidentialAddress", additionalProperties = Schema.AdditionalPropertiesValue.FALSE,
            description = "Endereço residencial brasileiro. Todos os campos exceto complement são necessários para completude, mas podem ser preenchidos depois.")
    public record Address(
            @Schema(description = "CEP; após aparar aceita 12345678 ou 12345-678 e retorna oito dígitos. Branco remove.", nullable = true) String postalCode,
            @Schema(description = "Logradouro. Até 150 caracteres após aparar; branco remove.", nullable = true) String street,
            @Schema(description = "Número, ou S/N para imóvel sem número. Até 20 caracteres após aparar; branco remove.", nullable = true) String number,
            @Schema(description = "Complemento opcional; não participa da completude. Até 150 caracteres após aparar; branco remove.", nullable = true) String complement,
            @Schema(description = "Bairro. Até 100 caracteres após aparar; branco remove.", nullable = true) String neighborhood,
            @Schema(description = "Cidade. Até 100 caracteres após aparar; branco remove.", nullable = true) String city,
            @Schema(description = "UF brasileira válida de duas letras após aparar; aceita maiúsculas/minúsculas e retorna maiúsculas. "
                    + "Branco remove. Valores canônicos: AC, AL, AP, AM, BA, CE, DF, ES, GO, MA, MT, MS, MG, PA, PB, PR, PE, PI, RJ, RN, RS, RO, RR, SC, SP, SE, TO.", nullable = true) String state) {
        public ResidentialAddress toDomain() {
            return new ResidentialAddress(postalCode, street, number, complement, neighborhood, city, state);
        }
    }

    public static UserProfileDataDTO read(JsonNode body) {
        object(body, Set.of("workProfile", "address"));
        JsonNode work = body.get("workProfile");
        JsonNode address = body.get("address");
        Work workData = null;
        Address addressData = null;
        if (work != null && !work.isNull()) {
            object(work, Set.of("occupation", "workplace"));
            workData = new Work(text(work, "occupation"), text(work, "workplace"));
        }
        if (address != null && !address.isNull()) {
            object(address, Set.of("postalCode", "street", "number", "complement", "neighborhood", "city", "state"));
            addressData = new Address(text(address, "postalCode"), text(address, "street"), text(address, "number"),
                    text(address, "complement"), text(address, "neighborhood"), text(address, "city"), text(address, "state"));
        }
        return new UserProfileDataDTO(workData, addressData);
    }

    private static void object(JsonNode node, Set<String> fields) {
        if (node == null || !node.isObject()) throw new IllegalArgumentException("Profile must be a JSON object");
        for (String field : node.propertyNames()) {
            if (!fields.contains(field)) throw new IllegalArgumentException("Unsupported profile field");
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return null;
        if (!value.isString()) throw new IllegalArgumentException("Profile field must be a string or null");
        return value.asString();
    }
}
