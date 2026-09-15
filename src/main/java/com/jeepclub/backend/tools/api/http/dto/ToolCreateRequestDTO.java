package com.jeepclub.backend.tools.api.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para criar uma ferramenta ACTIVE para o proprietário da operação.")
public record ToolCreateRequestDTO(

        @Schema(description = "Nome da ferramenta. É obrigatório e não é normalizado na criação.", example = "Chave de Roda Cruz", maxLength = 100, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "O nome da ferramenta é obrigatório")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String name,

        @Schema(description = "Descrição opcional da ferramenta. É persistida conforme recebida na criação.", example = "Chave de roda com encaixes 17mm, 19mm e 21mm.", maxLength = 500, nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
        String description
) {}
