package com.jeepclub.backend.tools.api.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload de atualização parcial: campo nulo preserva o valor atual.")
public record ToolUpdateRequestDTO(

        @Schema(description = "Novo nome opcional. Nulo ou vazio preserva o nome; texto não vazio é aparado antes de substituir.", example = "Chave de Roda Cruz Reforçada", maxLength = 100, nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String name,

        @Schema(description = "Nova descrição opcional. Nula preserva o valor; qualquer texto não nulo é aparado e substitui, inclusive texto vazio.", example = "Chave de roda pintada de preto.", maxLength = 500, nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
        String description
) {}
