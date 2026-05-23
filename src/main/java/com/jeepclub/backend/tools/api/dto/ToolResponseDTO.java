package com.jeepclub.backend.tools.api.dto;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import com.jeepclub.backend.tools.core.domain.model.Tool;


// o padrão pra uma dto não é usar um arquivo de class, e sim um arquivo de record.
// necessario aplicar documentação swagger na sua dto, ler as dtos da pasta api do modulo authorization
// para ver o padrão da documentação swagger que estamos usando
public record ToolResponseDTO(
        Long id,
        String name,
        String description,
        ToolStatus status,
        Long userId
) {}