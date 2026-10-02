package com.jeepclub.backend.publications.api.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Edição parcial: campo omitido preserva o atual; null explícito é inválido. "
        + "Galeria enviada substitui a anterior. ARCHIVED é somente leitura.",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record UpdateNoticeRequestDTO(
        @Schema(description = "Novo título. Omitir preserva.", maxLength = 200, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 200) String title,
        @Schema(description = "Novo conteúdo. Omitir preserva.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String content,
        @Schema(description = "Nova galeria completa, 1 a 5 imagens. Omitir preserva a galeria anterior.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(min = 1, max = 5) List<@NotNull @Valid PublicationImageRequestDTO> images
) { }
