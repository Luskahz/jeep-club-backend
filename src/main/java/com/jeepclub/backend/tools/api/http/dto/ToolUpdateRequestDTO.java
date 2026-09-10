package com.jeepclub.backend.tools.api.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Objeto de requisição para atualizar os dados de uma ferramenta existente.")
public record ToolUpdateRequestDTO(

        @Schema(description = "Novo nome da ferramenta (opcional).", example = "Chave de Roda Cruz Reforçada")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String name,

        @Schema(description = "Nova descrição da ferramenta (opcional).", example = "Chave de roda pintada de preto.")
        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
        String description
) {}
