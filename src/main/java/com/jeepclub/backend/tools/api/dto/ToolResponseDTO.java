package com.jeepclub.backend.tools.api.dto;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import com.jeepclub.backend.tools.core.domain.model.Tool;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Objeto de resposta com os dados completos de uma ferramenta.")
public record ToolResponseDTO(

        @Schema(description = "Identificador único da ferramenta no sistema.", example = "1")
        Long id,

        @Schema(description = "Nome da ferramenta ou equipamento.", example = "Macaco Hidráulico 2 Toneladas")
        String name,

        @Schema(description = "Descrição detalhada da ferramenta.", example = "Macaco tipo jacaré, cor vermelha, ideal para troca de pneus.")
        String description,

        @Schema(description = "Status atual de disponibilidade da ferramenta.", example = "AVAILABLE")
        ToolStatus status,

        @Schema(description = "Identificador único do usuário dono da ferramenta.", example = "42")
        Long userId

) {
    public ToolResponseDTO(Tool tool) {
        this(
                tool.getId(),
                tool.getName(),
                tool.getDescription(),
                tool.getStatus(),
                tool.getUserId()
        );
    }
}