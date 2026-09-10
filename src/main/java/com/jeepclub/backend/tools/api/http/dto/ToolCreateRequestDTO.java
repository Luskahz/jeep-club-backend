package com.jeepclub.backend.tools.api.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Objeto de requisição para criar uma nova ferramenta.")
public record ToolCreateRequestDTO(

        @Schema(description = "Nome da ferramenta a ser criada.", example = "Chave de Roda Cruz")
        @NotBlank(message = "O nome da ferramenta é obrigatório")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String name,

        @Schema(description = "Descrição detalhada da ferramenta.", example = "Chave de roda com encaixes 17mm, 19mm e 21mm.")
        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
        String description
) {}
