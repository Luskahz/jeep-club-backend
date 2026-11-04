package com.jeepclub.backend.tools.api.http.dto;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import com.jeepclub.backend.tools.core.domain.model.Tool;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados resumidos de uma ferramenta para listagem administrativa.")
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record AdminToolSummaryResponseDTO(
        @Schema(description = "Identificador único da ferramenta.", example = "42")
        Long id,
        @Schema(description = "Nome persistido da ferramenta.", example = "Macaco hidráulico")
        String name,
        @Schema(description = "Status operacional atual.", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "ARCHIVED"})
        ToolStatus status,
        @Schema(description = "Identificador do usuário dono da ferramenta.", example = "42")
        Long userId
) {
    public AdminToolSummaryResponseDTO(com.jeepclub.backend.tools.core.application.result.AdminToolResult result) {
        this(result.id(),result.name(),result.status(),result.userId());
    }
    public AdminToolSummaryResponseDTO(Tool tool) {
        this(tool.getId(), tool.getName(), tool.getStatus(), tool.getUserId());
    }
}
