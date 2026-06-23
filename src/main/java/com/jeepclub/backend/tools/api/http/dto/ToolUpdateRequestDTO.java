package com.jeepclub.backend.tools.api.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Objeto de requisição para atualizar os dados de uma ferramenta existente.")
public record ToolUpdateRequestDTO(

        @Schema(description = "Novo nome da ferramenta (opcional).", example = "Chave de Roda Cruz Reforçada")
        String name,

        @Schema(description = "Nova descrição da ferramenta (opcional).", example = "Chave de roda pintada de preto.")
        String description
) {}