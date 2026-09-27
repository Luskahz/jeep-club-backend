package com.jeepclub.backend.publications.api.http.dto;

import com.jeepclub.backend.publications.core.domain.model.PublicationImage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Referência a uma imagem já enviada por POST /media/images; nenhuma URL ou arquivo é aceito.",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record PublicationImageRequestDTO(
        @Schema(description = "Chave do storage global.", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg", maxLength = 255)
        @NotBlank @Size(max = 255) String storageKey,
        @Schema(description = "Posição contínua a partir de zero.", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
        @NotNull @Min(0) Integer position,
        @Schema(description = "Indica a imagem principal; exatamente uma na galeria.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Boolean primary
) {
    public PublicationImage toDomain() {
        return new PublicationImage(storageKey, position, primary);
    }
}
