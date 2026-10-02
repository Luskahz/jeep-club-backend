package com.jeepclub.backend.publications.api.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Criação de aviso em DRAFT. Autor, status e timestamps são definidos pelo servidor.",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record CreateNoticeRequestDTO(
        @Schema(description = "Título do aviso.", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
        @NotBlank @Size(max = 200) String title,
        @Schema(description = "Conteúdo textual do aviso.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String content,
        @Schema(description = "Galeria com 1 a 5 imagens, exatamente uma principal, posições contínuas e chaves únicas.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Size(min = 1, max = 5) List<@NotNull @Valid PublicationImageRequestDTO> images
) { }
