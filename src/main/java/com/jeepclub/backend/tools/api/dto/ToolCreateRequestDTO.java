package com.jeepclub.backend.tools.api.dto;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Objeto de requisição para criar uma nova ferramenta.")
public record ToolCreateRequestDTO(

        @Schema(description = "Nome da ferramenta a ser criada.", example = "Chave de Roda Cruz")
        @NotBlank(message = "O nome da ferramenta é obrigatório")
        String name,

        @Schema(description = "Descrição detalhada da ferramenta.", example = "Chave de roda com encaixes 17mm, 19mm, 21mm e 23mm.")
        String description,

        // esse status realmente é obrigatorio? o frontend sabe os status existentes? não peça isso ao frontend,
        // trate o status como algo privado do backend isso não pode ficar esposto alteração de status deve passar previamente
        // por validação interna, quem altera status é o model. o user pede pra alterar via Requisição
        // traga rotas que alteram o status, sem pedir o status.
        @Schema(description = "Status inicial da ferramenta.", example = "AVAILABLE")
        @NotNull(message = "O status da ferramenta é obrigatório")
        ToolStatus status
) {}