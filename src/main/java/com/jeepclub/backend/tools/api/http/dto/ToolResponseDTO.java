package com.jeepclub.backend.tools.api.http.dto;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import com.jeepclub.backend.tools.core.domain.model.Tool;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados completos da ferramenta operacional.")
public record ToolResponseDTO(

        @Schema(description = "Identificador único da ferramenta no sistema.", example = "1")
        Long id,

        @Schema(description = "Nome da ferramenta ou equipamento.", example = "Macaco Hidráulico 2 Toneladas")
        String name,

        @Schema(description = "Descrição detalhada da ferramenta.", example = "Macaco tipo jacaré, cor vermelha, ideal para troca de pneus.")
        String description,

        @Schema(description = "Status operacional atual.", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
        ToolStatus status,

        @Schema(description = "Identificador único do usuário dono da ferramenta.", example = "42")
        Long userId,

        @Schema(description = "Chave da imagem no storage global; GET /media/images?key=... resolve a imagem.", nullable = true)
        String photoStorageKey

) {
    public ToolResponseDTO(Tool tool) {
        this(
                tool.getId(),
                tool.getName(),
                tool.getDescription(),
                tool.getStatus(),
                tool.getUserId(),
                tool.getPhotoStorageKey()
        );
    }
}
