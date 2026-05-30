package com.jeepclub.backend.tools.api.dto;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import com.jeepclub.backend.tools.core.domain.model.Tool;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Objeto de resposta com os dados resumidos de uma ferramenta para listagem.")
public record ToolSummaryResponseDTO(

        @Schema(description = "Identificador único da ferramenta.", example = "1")
        Long id,

        @Schema(description = "Nome da ferramenta.", example = "Macaco Hidráulico")
        String name,

        @Schema(description = "Status atual da ferramenta.", example = "AVAILABLE")
        ToolStatus status
) {
    // Construtor prático para converter a Entidade no Record de forma limpa
    public ToolSummaryResponseDTO(Tool tool) {
        this(tool.getId(), tool.getName(), tool.getStatus());
    }
}