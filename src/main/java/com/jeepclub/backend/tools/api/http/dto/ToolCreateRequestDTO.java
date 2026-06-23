package com.jeepclub.backend.tools.api.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Objeto de requisição para criar uma nova ferramenta.")
public record ToolCreateRequestDTO(

        @Schema(description = "Nome da ferramenta a ser criada.", example = "Chave de Roda Cruz")
        @NotBlank(message = "O nome da ferramenta é obrigatório")
        String name,

        @Schema(description = "Descrição detalhada da ferramenta.", example = "Chave de roda com encaixes 17mm, 19mm e 21mm.")
        String description
) {}