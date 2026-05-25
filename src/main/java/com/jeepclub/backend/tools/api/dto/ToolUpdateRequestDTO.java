package com.jeepclub.backend.tools.api.dto;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Objeto de requisição para atualizar os dados de uma ferramenta existente.")
public record ToolUpdateRequestDTO(

        @Schema(description = "Novo nome da ferramenta (opcional).", example = "Chave de Roda Cruz Reforçada")
        String name,

        @Schema(description = "Nova descrição da ferramenta (opcional).", example = "Chave de roda em aço carbono, pintada de preto.")
        String description,

        @Schema(description = "Novo status da ferramenta (opcional).", example = "IN_USE")
        ToolStatus status
) {}