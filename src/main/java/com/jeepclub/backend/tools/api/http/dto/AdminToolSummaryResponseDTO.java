package com.jeepclub.backend.tools.api.http.dto;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import com.jeepclub.backend.tools.core.domain.model.Tool;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados resumidos de uma ferramenta para listagem administrativa.")
public record AdminToolSummaryResponseDTO(
        @Schema(description = "Identificador único da ferramenta.", example = "42")
        Long id,
        @Schema(description = "Nome persistido da ferramenta.", example = "Macaco hidráulico")
        String name,
        @Schema(description = "Status operacional atual.", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
        ToolStatus status,
        @Schema(description = "Identificador do usuário dono da ferramenta.", example = "42")
        Long userId
) {
    public AdminToolSummaryResponseDTO(Tool tool) {
        this(tool.getId(), tool.getName(), tool.getStatus(), tool.getUserId());
    }
}
