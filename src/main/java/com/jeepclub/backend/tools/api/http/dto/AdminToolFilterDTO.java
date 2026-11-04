package com.jeepclub.backend.tools.api.http.dto;

import com.jeepclub.backend.tools.core.application.query.AdminToolFilter;
import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description="Filtros AND; q pesquisa nome. Texto literal, trim e case-insensitive; vazio não filtra.")
public record AdminToolFilterDTO(
        @Size(max=100) @Schema(description="Busca geral no nome, sem curingas LIKE.") String q,
        @Size(max=100) String name,
        ToolStatus status,
        @Positive @Schema(description="Identificador exato do proprietário, sem consulta cadastral.") Long userId) {
    public AdminToolFilter toFilter() { return new AdminToolFilter(q,name,status,userId); }
}
